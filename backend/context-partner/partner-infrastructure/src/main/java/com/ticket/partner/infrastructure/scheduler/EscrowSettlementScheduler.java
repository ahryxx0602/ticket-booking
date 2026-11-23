package com.ticket.partner.infrastructure.scheduler;

import com.ticket.partner.application.usecase.PartnerQueryService;
import com.ticket.partner.domain.model.OperatorWallet;
import com.ticket.partner.domain.model.WalletTransaction;
import com.ticket.partner.domain.model.WalletTransactionType;
import com.ticket.partner.domain.port.OperatorWalletRepository;
import com.ticket.partner.domain.port.PartnerCatalogPort;
import com.ticket.partner.domain.port.UnsettledTripDto;
import com.ticket.partner.domain.port.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EscrowSettlementScheduler {

    private final PartnerCatalogPort partnerCatalogPort;
    private final PartnerQueryService partnerQueryService;
    private final OperatorWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Scheduled(fixedDelay = 60000)
    public void processEscrowSettlements() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(2);
        List<UnsettledTripDto> unsettledTrips = partnerCatalogPort.findUnsettledTripsBefore(threshold);

        if (unsettledTrips.isEmpty()) {
            return;
        }

        log.info("Found {} completed trips ready for escrow settlement (departure <= {})",
                unsettledTrips.size(), threshold);

        for (UnsettledTripDto trip : unsettledTrips) {
            try {
                settleTrip(trip);
            } catch (Exception ex) {
                log.error("Failed to settle trip ID={}: {}", trip.tripId(), ex.getMessage(), ex);
            }
        }
    }

    @Transactional
    public void settleTrip(UnsettledTripDto trip) {
        Long tripId = trip.tripId();
        Long operatorId = trip.operatorId();

        if (operatorId == null) {
            partnerCatalogPort.markTripSettled(tripId);
            return;
        }

        BigDecimal grossSales = partnerCatalogPort.getTotalTicketSalesByTripId(tripId);
        if (grossSales == null || grossSales.compareTo(BigDecimal.ZERO) <= 0) {
            partnerCatalogPort.markTripSettled(tripId);
            log.info("Trip ID={} has zero ticket sales. Marked SETTLED without balance change.", tripId);
            return;
        }

        OperatorWallet wallet = walletRepository.findByOperatorId(operatorId).orElse(null);
        if (wallet == null) {
            log.error("Cannot settle trip ID={}: Operator ID={} wallet not found", tripId, operatorId);
            return;
        }

        // Atomic Check-and-Set: Chỉ worker nào chuyển trạng thái UNSETTLED -> SETTLED thành công mới được cộng tiền ví (ADR-14)
        boolean acquired = partnerCatalogPort.markTripSettledIfUnsettled(tripId);
        if (!acquired) {
            log.warn("Trip ID={} is already settled or in-progress by another instance. Skipping.", tripId);
            return;
        }

        BigDecimal rate = partnerQueryService.getEffectiveCommissionRate(operatorId);
        BigDecimal commission = grossSales.multiply(rate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal netAmount = grossSales.subtract(commission);

        // Chuyển tiền từ pending_settlement sang available_balance (tự động cấn trừ nợ nếu có)
        wallet.settleToAvailable(netAmount);
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .walletId(wallet.getId())
                .operatorId(operatorId)
                .tripId(tripId)
                .transactionType(WalletTransactionType.TRIP_SETTLEMENT)
                .grossAmount(grossSales)
                .commissionRate(rate)
                .commissionAmount(commission)
                .netAmount(netAmount)
                .balanceAfter(wallet.getAvailableBalance())
                .description("Trip completed escrow settlement (+2h after departure). Net credited to available balance.")
                .build();
        transactionRepository.save(tx);

        log.info("Successfully settled trip ID={} for operator ID={}: Gross={}, Rate={}%, Net={}, AvailBal={}, DebtBal={}",
                tripId, operatorId, grossSales, rate, netAmount, wallet.getAvailableBalance(), wallet.getDebtBalance());
    }
}
