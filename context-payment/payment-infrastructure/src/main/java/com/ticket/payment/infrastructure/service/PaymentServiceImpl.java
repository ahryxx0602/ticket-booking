package com.ticket.payment.infrastructure.service;

import com.ticket.payment.application.dto.CreatePaymentCommand;
import com.ticket.payment.application.dto.PaymentInitResponse;
import com.ticket.payment.application.dto.VnPayCallbackDto;
import com.ticket.payment.application.dto.VnPayIpnResponse;
import com.ticket.payment.application.usecase.CreatePaymentUseCase;
import com.ticket.payment.application.usecase.HandlePaymentCallbackUseCase;
import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.domain.port.PaymentBookingConfirmPort;
import com.ticket.payment.domain.port.PaymentBookingPort;
import com.ticket.payment.domain.port.PaymentIdentityPort;
import com.ticket.payment.domain.repository.PaymentRepository;
import com.ticket.payment.infrastructure.config.VnPayProperties;
import com.ticket.payment.infrastructure.crypto.VnPaySignUtil;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import com.ticket.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements CreatePaymentUseCase, HandlePaymentCallbackUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentBookingPort paymentBookingPort;
    private final PaymentBookingConfirmPort paymentBookingConfirmPort;
    private final PaymentIdentityPort paymentIdentityPort;
    private final VnPayProperties vnPayProperties;

    @Override
    @Transactional
    public PaymentInitResponse execute(CreatePaymentCommand command, String username, String clientIp) {
        if (command.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new BusinessException("UNSUPPORTED_PAYMENT_METHOD", "Only VNPAY payment method is supported in this phase");
        }

        Long currentUserId = paymentIdentityPort.findActiveUserIdByUsername(username)
                .orElseThrow(() -> new ForbiddenException("ACCOUNT_LOCKED", "User account is locked or does not exist"));

        PaymentBookingPort.BookingSnapshot booking = paymentBookingPort.findBookingByReference(command.getBookingReference())
                .orElseThrow(() -> new EntityNotFoundException("Booking", command.getBookingReference()));

        if (!booking.userId().equals(currentUserId)) {
            throw new ForbiddenException("BOOKING_ACCESS_DENIED", "You do not have permission to pay for this booking");
        }

        Optional<Payment> successPayment = paymentRepository.findFirstByBookingIdAndStatus(booking.id(), PaymentStatus.SUCCESS);
        if (successPayment.isPresent()) {
            throw new BusinessException("BOOKING_ALREADY_PAID", "Booking has already been paid successfully");
        }

        if (!booking.isHeld()) {
            throw new BusinessException("INVALID_BOOKING_STATUS", "Only HELD bookings can be paid: " + booking.status());
        }

        LocalDateTime now = LocalDateTime.now();
        if (booking.isExpired(now) || Duration.between(now, booking.expiresAt()).getSeconds() < 60) {
            throw new BusinessException("BOOKING_EXPIRING_SOON", "Booking hold period is expiring soon or has already expired");
        }

        Optional<Payment> existingPending = paymentRepository.findFirstByBookingIdAndStatus(booking.id(), PaymentStatus.PENDING);
        if (existingPending.isPresent()) {
            Payment p = existingPending.get();
            if (p.getCreatedAt() != null && p.getCreatedAt().plusMinutes(10).isAfter(now)) {
                String reuseUrl = VnPaySignUtil.generatePaymentUrl(
                        vnPayProperties,
                        p.getPaymentReference(),
                        p.getAmount(),
                        "Payment for booking " + booking.bookingReference(),
                        clientIp,
                        p.getCreatedAt(),
                        booking.expiresAt()
                );
                return PaymentInitResponse.builder()
                        .paymentReference(p.getPaymentReference())
                        .redirectPaymentUrl(reuseUrl)
                        .build();
            } else {
                p.markFailed();
                paymentRepository.save(p);
            }
        }

        String paymentRef = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Payment payment = Payment.builder()
                .paymentReference(paymentRef)
                .bookingId(booking.id())
                .amount(booking.totalAmount())
                .currency("VND")
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        String redirectUrl = VnPaySignUtil.generatePaymentUrl(
                vnPayProperties,
                paymentRef,
                booking.totalAmount(),
                "Payment for booking " + booking.bookingReference(),
                clientIp,
                now,
                booking.expiresAt()
        );

        return PaymentInitResponse.builder()
                .paymentReference(paymentRef)
                .redirectPaymentUrl(redirectUrl)
                .build();
    }

    @Override
    @Transactional
    public VnPayIpnResponse execute(VnPayCallbackDto callbackDto) {
        Map<String, String> allParams = callbackDto.getAllParams();
        if (allParams == null || allParams.isEmpty()) {
            log.warn("VNPay IPN callback parameters are null or empty");
            return VnPayIpnResponse.invalidChecksum();
        }

        boolean validSignature = VnPaySignUtil.verifySignature(allParams, vnPayProperties.getHashSecret());
        if (!validSignature) {
            log.warn("Invalid VNPay IPN signature for TxnRef: {}", callbackDto.getVnp_TxnRef());
            return VnPayIpnResponse.invalidChecksum();
        }

        Optional<Payment> optPayment = paymentRepository.findByPaymentReferenceForUpdate(callbackDto.getVnp_TxnRef());
        if (optPayment.isEmpty()) {
            log.warn("Payment reference not found for IPN: {}", callbackDto.getVnp_TxnRef());
            return VnPayIpnResponse.orderNotFound();
        }
        Payment payment = optPayment.get();

        String vnpAmountStr = callbackDto.getVnp_Amount() != null ? callbackDto.getVnp_Amount() : allParams.get("vnp_Amount");
        if (vnpAmountStr == null || vnpAmountStr.isBlank()) {
            log.warn("Missing vnp_Amount in IPN for TxnRef: {}", payment.getPaymentReference());
            return VnPayIpnResponse.invalidAmount();
        }

        try {
            long receivedAmount = Long.parseLong(vnpAmountStr.trim());
            long expectedAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
            if (receivedAmount != expectedAmount) {
                log.warn("Amount mismatch in IPN for TxnRef {}: received {} vs expected {}",
                        payment.getPaymentReference(), receivedAmount, expectedAmount);
                return VnPayIpnResponse.invalidAmount();
            }
        } catch (Exception ex) {
            log.warn("Error parsing vnp_Amount {}: {}", vnpAmountStr, ex.getMessage());
            return VnPayIpnResponse.invalidAmount();
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Payment reference {} already confirmed/refunded (status: {})", payment.getPaymentReference(), payment.getStatus());
            return VnPayIpnResponse.alreadyConfirmed();
        }

        if (payment.getStatus() == PaymentStatus.FAILED) {
            if ("00".equals(callbackDto.getVnp_ResponseCode()) && "00".equals(callbackDto.getVnp_TransactionStatus())) {
                log.warn("Payment reference {} was marked FAILED, but bank confirmed success. Marking reconciliation.", payment.getPaymentReference());
                payment.markReconciliationRequired(callbackDto.getVnp_TransactionNo(), "SUCCESS_CALLBACK_AFTER_FAILED_STATUS");
                paymentRepository.save(payment);
                return VnPayIpnResponse.success();
            } else {
                return VnPayIpnResponse.success();
            }
        }

        LocalDateTime now = LocalDateTime.now();
        if ("00".equals(callbackDto.getVnp_ResponseCode()) && "00".equals(callbackDto.getVnp_TransactionStatus())) {
            PaymentBookingConfirmPort.BookingConfirmResult confirmResult =
                    paymentBookingConfirmPort.confirmBooking(payment.getBookingId(), now);

            if (confirmResult == PaymentBookingConfirmPort.BookingConfirmResult.ALREADY_CONFIRMED) {
                payment.markReconciliationRequired(callbackDto.getVnp_TransactionNo(), "DUPLICATE_PAYMENT_FOR_CONFIRMED_BOOKING");
                paymentRepository.save(payment);
                log.warn("Payment {} succeeded but booking was ALREADY CONFIRMED. Marked for reconciliation.", payment.getPaymentReference());
                return VnPayIpnResponse.success();
            }

            if (confirmResult == PaymentBookingConfirmPort.BookingConfirmResult.EXPIRED_RECONCILIATION_REQUIRED) {
                payment.markReconciliationRequired(callbackDto.getVnp_TransactionNo(), "PAID_AFTER_BOOKING_EXPIRED");
                paymentRepository.save(payment);
                log.warn("Payment {} succeeded but booking was EXPIRED. Marked for reconciliation.", payment.getPaymentReference());
                return VnPayIpnResponse.success();
            }

            payment.markSuccess(callbackDto.getVnp_TransactionNo());
            paymentRepository.save(payment);
            log.info("Payment reference {} succeeded with TxnNo: {}", payment.getPaymentReference(), callbackDto.getVnp_TransactionNo());
            return VnPayIpnResponse.success();
        } else {
            payment.markFailed();
            paymentRepository.save(payment);
            log.warn("Payment reference {} failed with response code: {}", payment.getPaymentReference(), callbackDto.getVnp_ResponseCode());
            return VnPayIpnResponse.success();
        }
    }
}
