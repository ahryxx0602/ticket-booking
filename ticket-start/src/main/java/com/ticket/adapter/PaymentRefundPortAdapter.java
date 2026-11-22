package com.ticket.adapter;

import com.ticket.booking.domain.port.PaymentRefundPort;
import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentRefundPortAdapter implements PaymentRefundPort {

    private final PaymentRepository paymentRepository;

    @Override
    public boolean refundByBookingId(Long bookingId, BigDecimal refundAmount, String reason) {
        Optional<Payment> paymentOpt = paymentRepository.findFirstByBookingIdAndStatusOrderByIdDesc(
                bookingId,
                PaymentStatus.SUCCESS
        );
        if (paymentOpt.isEmpty()) {
            // Phase 3 policy: allow booking cancellation when no Phase 4 payment record exists yet
            return false;
        }
        Payment payment = paymentOpt.get();
        payment.markRefunded(refundAmount, reason);
        paymentRepository.save(payment);
        return true;
    }
}
