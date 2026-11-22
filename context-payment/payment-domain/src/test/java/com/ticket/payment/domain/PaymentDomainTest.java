package com.ticket.payment.domain;

import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentDomainTest {

    @Test
    void markSuccess_whenPending_shouldSucceed() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_1")
                .bookingId(100L)
                .amount(BigDecimal.valueOf(1500000))
                .status(PaymentStatus.PENDING)
                .build();

        payment.markSuccess("VNP_12345");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionId()).isEqualTo("VNP_12345");
        assertThat(payment.getPaymentTime()).isNotNull();
    }

    @Test
    void markSuccess_whenNotPending_shouldThrowInvalidPaymentStatus() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_2")
                .status(PaymentStatus.SUCCESS)
                .build();

        assertThatThrownBy(() -> payment.markSuccess("VNP_12345"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_PAYMENT_STATUS");
    }

    @Test
    void markFailed_whenPending_shouldSucceed() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_3")
                .status(PaymentStatus.PENDING)
                .build();

        payment.markFailed();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void markFailed_whenNotPending_shouldThrowInvalidPaymentStatus() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_4")
                .status(PaymentStatus.REFUNDED)
                .build();

        assertThatThrownBy(payment::markFailed)
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_PAYMENT_STATUS");
    }

    @Test
    void markReconciliationRequired_shouldMarkSuccessAndReconciliationFlag() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_5")
                .status(PaymentStatus.PENDING)
                .build();

        payment.markReconciliationRequired("PAID_AFTER_BOOKING_EXPIRED");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.isReconciliationRequired()).isTrue();
        assertThat(payment.getRefundReason()).isEqualTo("PAID_AFTER_BOOKING_EXPIRED");
        assertThat(payment.getPaymentTime()).isNotNull();
    }

    @Test
    void markRefunded_whenSuccessAndValidAmount_shouldSucceed() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_6")
                .amount(BigDecimal.valueOf(2000000))
                .status(PaymentStatus.SUCCESS)
                .build();

        payment.markRefunded(BigDecimal.valueOf(1000000), "Customer cancellation refund");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        assertThat(payment.getRefundReason()).isEqualTo("Customer cancellation refund");
        assertThat(payment.getRefundedAt()).isNotNull();
    }

    @Test
    void markRefunded_whenNotSuccess_shouldThrowInvalidPaymentStatus() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_7")
                .amount(BigDecimal.valueOf(2000000))
                .status(PaymentStatus.PENDING)
                .build();

        assertThatThrownBy(() -> payment.markRefunded(BigDecimal.valueOf(1000000), "Reason"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_PAYMENT_STATUS");
    }

    @Test
    void markRefunded_whenRefundAmountExceedsOriginal_shouldThrowInvalidRefundAmount() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_8")
                .amount(BigDecimal.valueOf(2000000))
                .status(PaymentStatus.SUCCESS)
                .build();

        assertThatThrownBy(() -> payment.markRefunded(BigDecimal.valueOf(2500000), "Reason"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_REFUND_AMOUNT");
    }

    @Test
    void markRefunded_whenReasonBlank_shouldThrowInvalidRefundReason() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_9")
                .amount(BigDecimal.valueOf(2000000))
                .status(PaymentStatus.SUCCESS)
                .build();

        assertThatThrownBy(() -> payment.markRefunded(BigDecimal.valueOf(1000000), "   "))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_REFUND_REASON");
    }
}
