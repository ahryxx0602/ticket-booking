package com.ticket.partner.application.listener;

import com.ticket.partner.application.usecase.PartnerQueryService;
import com.ticket.partner.domain.model.OperatorWallet;
import com.ticket.partner.domain.model.WalletTransaction;
import com.ticket.partner.domain.model.WalletTransactionType;
import com.ticket.partner.domain.port.OperatorWalletRepository;
import com.ticket.partner.domain.port.PartnerCatalogPort;
import com.ticket.partner.domain.port.WalletTransactionRepository;
import com.ticket.shared.event.BookingRefundedEvent;
import com.ticket.shared.event.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartnerPaymentEventListener {

    private final PartnerCatalogPort partnerCatalogPort;
    private final PartnerQueryService partnerQueryService;
    private final OperatorWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("Received PaymentConfirmedEvent: BookingRef={}, TripId={}, Amount={}",
                event.bookingReference(), event.tripId(), event.totalAmount());

        if (event.tripId() == null) {
            return;
        }

        Optional<Long> operatorIdOpt = partnerCatalogPort.findOperatorIdByTripId(event.tripId());
        if (operatorIdOpt.isEmpty()) {
            log.debug("Trip ID={} is not assigned to any bus operator. Skipping partner wallet escrow.", event.tripId());
            return;
        }

        Long operatorId = operatorIdOpt.get();
        BigDecimal grossAmount = event.totalAmount() != null ? event.totalAmount() : BigDecimal.ZERO;
        if (grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal rate = partnerQueryService.getEffectiveCommissionRate(operatorId);
        BigDecimal commissionAmount = grossAmount.multiply(rate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal netAmount = grossAmount.subtract(commissionAmount);

        // Atomic SQL Update vào ví (ADR-15)
        walletRepository.creditPendingAtomic(operatorId, netAmount);

        OperatorWallet wallet = walletRepository.findByOperatorId(operatorId).orElse(null);
        Long walletId = wallet != null ? wallet.getId() : null;
        BigDecimal currentBal = wallet != null ? wallet.getAvailableBalance() : BigDecimal.ZERO;

        WalletTransaction tx = WalletTransaction.builder()
                .walletId(walletId)
                .operatorId(operatorId)
                .tripId(event.tripId())
                .bookingId(event.bookingId())
                .paymentReference(event.paymentReference())
                .transactionType(WalletTransactionType.TICKET_ESCROW_CREDIT)
                .grossAmount(grossAmount)
                .commissionRate(rate)
                .commissionAmount(commissionAmount)
                .netAmount(netAmount)
                .balanceAfter(currentBal)
                .description("Ticket sale escrow credit. Gross: " + grossAmount + " VND, Rate: " + rate + "%, Net: " + netAmount + " VND")
                .build();
        transactionRepository.save(tx);

        log.info("Escrow pending credited: Operator ID={}, Net Amount={}, Rate={}% for Booking #{}",
                operatorId, netAmount, rate, event.bookingReference());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBookingRefunded(BookingRefundedEvent event) {
        log.info("Received BookingRefundedEvent: BookingRef={}, TripId={}, RefundAmount={}",
                event.bookingReference(), event.tripId(), event.refundAmount());

        if (event.tripId() == null) {
            return;
        }

        Optional<Long> operatorIdOpt = partnerCatalogPort.findOperatorIdByTripId(event.tripId());
        if (operatorIdOpt.isEmpty()) {
            return;
        }

        Long operatorId = operatorIdOpt.get();
        OperatorWallet wallet = walletRepository.findByOperatorId(operatorId).orElse(null);
        if (wallet == null) {
            log.error("Cannot deduct refund: Operator ID={} wallet not found", operatorId);
            return;
        }

        BigDecimal grossRefund = event.refundAmount() != null ? event.refundAmount() : BigDecimal.ZERO;
        BigDecimal rate = partnerQueryService.getEffectiveCommissionRate(operatorId);
        BigDecimal commissionPortion = grossRefund.multiply(rate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal netDeduction = grossRefund.subtract(commissionPortion);

        boolean isSettled = partnerCatalogPort.isTripSettled(event.tripId());
        wallet.refundDeduction(netDeduction, !isSettled);
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .walletId(wallet.getId())
                .operatorId(operatorId)
                .tripId(event.tripId())
                .bookingId(event.bookingId())
                .transactionType(WalletTransactionType.REFUND_REVERSAL)
                .grossAmount(grossRefund)
                .commissionRate(rate)
                .commissionAmount(commissionPortion)
                .netAmount(netDeduction.negate())
                .balanceAfter(wallet.getAvailableBalance())
                .description("Refund deduction. Net deduction: " + netDeduction + " VND, Trip Settled: " + isSettled)
                .build();
        transactionRepository.save(tx);

        log.info("Refund deducted from wallet: Operator ID={}, Net={}, Trip Settled={}, New Debt={}",
                operatorId, netDeduction, isSettled, wallet.getDebtBalance());
    }
}
