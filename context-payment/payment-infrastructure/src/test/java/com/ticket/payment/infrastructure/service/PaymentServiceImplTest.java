package com.ticket.payment.infrastructure.service;

import com.ticket.payment.application.dto.CreatePaymentCommand;
import com.ticket.payment.application.dto.PaymentInitResponse;
import com.ticket.payment.application.dto.VnPayCallbackDto;
import com.ticket.payment.application.dto.VnPayIpnResponse;
import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.domain.port.PaymentBookingConfirmPort;
import com.ticket.payment.domain.port.PaymentBookingPort;
import com.ticket.payment.domain.port.PaymentIdentityPort;
import com.ticket.payment.domain.repository.PaymentRepository;
import com.ticket.payment.infrastructure.config.VnPayProperties;
import com.ticket.payment.infrastructure.crypto.HmacSha512Util;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentBookingPort paymentBookingPort;
    @Mock
    private PaymentBookingConfirmPort paymentBookingConfirmPort;
    @Mock
    private PaymentIdentityPort paymentIdentityPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private VnPayProperties vnPayProperties;
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        vnPayProperties = new VnPayProperties();
        vnPayProperties.setHashSecret("secret_key_for_test");
        vnPayProperties.setTmnCode("TMN_CODE");
        vnPayProperties.setPayUrl("https://sandbox.vnpayment.vn/pay");
        vnPayProperties.setReturnUrl("http://localhost:8080/return");

        paymentService = new PaymentServiceImpl(
                paymentRepository,
                paymentBookingPort,
                paymentBookingConfirmPort,
                paymentIdentityPort,
                vnPayProperties,
                eventPublisher
        );
    }

    @Test
    void createPayment_whenValid_shouldReturnInitResponse() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingReference("BKG_123456")
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        PaymentBookingPort.BookingSnapshot snapshot = new PaymentBookingPort.BookingSnapshot(
                100L,
                "BKG_123456",
                10L,
                BigDecimal.valueOf(1500000),
                "HELD",
                LocalDateTime.now().plusMinutes(8)
        );

        when(paymentIdentityPort.findActiveUserIdByUsername("customer1")).thenReturn(Optional.of(10L));
        when(paymentBookingPort.findBookingByReference("BKG_123456")).thenReturn(Optional.of(snapshot));
        when(paymentRepository.findFirstByBookingIdAndStatus(100L, PaymentStatus.SUCCESS)).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBookingIdAndStatus(100L, PaymentStatus.PENDING)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentInitResponse response = paymentService.execute(command, "customer1", "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getPaymentReference()).startsWith("PAY_");
        assertThat(response.getRedirectPaymentUrl()).contains("vnp_Amount=150000000");
    }

    @Test
    void createPayment_whenDifferentUser_shouldThrowForbiddenException() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingReference("BKG_123456")
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        PaymentBookingPort.BookingSnapshot snapshot = new PaymentBookingPort.BookingSnapshot(
                100L,
                "BKG_123456",
                10L, // Owner is 10
                BigDecimal.valueOf(1500000),
                "HELD",
                LocalDateTime.now().plusMinutes(8)
        );

        when(paymentIdentityPort.findActiveUserIdByUsername("attacker")).thenReturn(Optional.of(99L)); // Attacker is 99
        when(paymentBookingPort.findBookingByReference("BKG_123456")).thenReturn(Optional.of(snapshot));

        assertThatThrownBy(() -> paymentService.execute(command, "attacker", "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo("BOOKING_ACCESS_DENIED");
    }

    @Test
    void createPayment_whenExpiringSoon_shouldThrowBookingExpiringSoon() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingReference("BKG_123456")
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        PaymentBookingPort.BookingSnapshot snapshot = new PaymentBookingPort.BookingSnapshot(
                100L,
                "BKG_123456",
                10L,
                BigDecimal.valueOf(1500000),
                "HELD",
                LocalDateTime.now().plusSeconds(30) // Only 30s remaining
        );

        when(paymentIdentityPort.findActiveUserIdByUsername("customer1")).thenReturn(Optional.of(10L));
        when(paymentBookingPort.findBookingByReference("BKG_123456")).thenReturn(Optional.of(snapshot));

        assertThatThrownBy(() -> paymentService.execute(command, "customer1", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("BOOKING_EXPIRING_SOON");
    }

    @Test
    void handleCallback_whenInvalidChecksum_shouldReturn97() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "PAY_001");
        params.put("vnp_SecureHash", "invalid_hash");

        VnPayCallbackDto dto = new VnPayCallbackDto();
        dto.setVnp_TxnRef("PAY_001");
        dto.setAllParams(params);

        VnPayIpnResponse response = paymentService.execute(dto);

        assertThat(response.getRspCode()).isEqualTo("97");
    }

    @Test
    void handleCallback_whenOrderNotFound_shouldReturn01() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "PAY_NOT_FOUND");
        String signData = "vnp_TxnRef=PAY_NOT_FOUND";
        params.put("vnp_SecureHash", HmacSha512Util.hmacSHA512("secret_key_for_test", signData));

        VnPayCallbackDto dto = new VnPayCallbackDto();
        dto.setVnp_TxnRef("PAY_NOT_FOUND");
        dto.setAllParams(params);

        when(paymentRepository.findByPaymentReferenceForUpdate("PAY_NOT_FOUND")).thenReturn(Optional.empty());

        VnPayIpnResponse response = paymentService.execute(dto);

        assertThat(response.getRspCode()).isEqualTo("01");
    }

    @Test
    void handleCallback_whenSuccessAndValid_shouldConfirmAndReturn00() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_100")
                .bookingId(500L)
                .amount(BigDecimal.valueOf(1000000))
                .status(PaymentStatus.PENDING)
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "TXN_777");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "PAY_100");

        String signData = "vnp_Amount=100000000&vnp_ResponseCode=00&vnp_TransactionNo=TXN_777&vnp_TransactionStatus=00&vnp_TxnRef=PAY_100";
        params.put("vnp_SecureHash", HmacSha512Util.hmacSHA512("secret_key_for_test", signData));

        VnPayCallbackDto dto = new VnPayCallbackDto();
        dto.setVnp_TxnRef("PAY_100");
        dto.setVnp_Amount("100000000");
        dto.setVnp_ResponseCode("00");
        dto.setVnp_TransactionStatus("00");
        dto.setVnp_TransactionNo("TXN_777");
        dto.setAllParams(params);

        when(paymentRepository.findByPaymentReferenceForUpdate("PAY_100")).thenReturn(Optional.of(payment));
        when(paymentBookingConfirmPort.confirmBooking(eq(500L), any(LocalDateTime.class)))
                .thenReturn(PaymentBookingConfirmPort.BookingConfirmResult.CONFIRMED);

        VnPayIpnResponse response = paymentService.execute(dto);

        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionId()).isEqualTo("TXN_777");
        verify(paymentRepository).save(payment);
    }

    @Test
    void handleCallback_whenSuccessAfterExpired_shouldMarkReconciliationAndReturn00() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_101")
                .bookingId(501L)
                .amount(BigDecimal.valueOf(1000000))
                .status(PaymentStatus.PENDING)
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "TXN_888");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "PAY_101");

        String signData = "vnp_Amount=100000000&vnp_ResponseCode=00&vnp_TransactionNo=TXN_888&vnp_TransactionStatus=00&vnp_TxnRef=PAY_101";
        params.put("vnp_SecureHash", HmacSha512Util.hmacSHA512("secret_key_for_test", signData));

        VnPayCallbackDto dto = new VnPayCallbackDto();
        dto.setVnp_TxnRef("PAY_101");
        dto.setVnp_Amount("100000000");
        dto.setVnp_ResponseCode("00");
        dto.setVnp_TransactionStatus("00");
        dto.setVnp_TransactionNo("TXN_888");
        dto.setAllParams(params);

        when(paymentRepository.findByPaymentReferenceForUpdate("PAY_101")).thenReturn(Optional.of(payment));
        when(paymentBookingConfirmPort.confirmBooking(eq(501L), any(LocalDateTime.class)))
                .thenReturn(PaymentBookingConfirmPort.BookingConfirmResult.EXPIRED_RECONCILIATION_REQUIRED);

        VnPayIpnResponse response = paymentService.execute(dto);

        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.isReconciliationRequired()).isTrue();
        assertThat(payment.getRefundReason()).isEqualTo("PAID_AFTER_BOOKING_EXPIRED");
        verify(paymentRepository).save(payment);
    }

    @Test
    void handleCallback_whenAlreadyConfirmed_shouldBeIdempotentAndReturn00() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_102")
                .bookingId(502L)
                .amount(BigDecimal.valueOf(1000000))
                .status(PaymentStatus.SUCCESS)
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_TxnRef", "PAY_102");
        String signData = "vnp_Amount=100000000&vnp_TxnRef=PAY_102";
        params.put("vnp_SecureHash", HmacSha512Util.hmacSHA512("secret_key_for_test", signData));

        VnPayCallbackDto dto = new VnPayCallbackDto();
        dto.setVnp_TxnRef("PAY_102");
        dto.setVnp_Amount("100000000");
        dto.setAllParams(params);

        when(paymentRepository.findByPaymentReferenceForUpdate("PAY_102")).thenReturn(Optional.of(payment));

        VnPayIpnResponse response = paymentService.execute(dto);

        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(response.getMessage()).isEqualTo("Order already confirmed");
        verify(paymentBookingConfirmPort, never()).confirmBooking(any(), any());
    }

    @Test
    void handleCallback_whenAmountMismatch_shouldReturn04() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_103")
                .bookingId(503L)
                .amount(BigDecimal.valueOf(1000000))
                .status(PaymentStatus.PENDING)
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "200000000"); // 2,000,000 instead of 1,000,000
        params.put("vnp_TxnRef", "PAY_103");
        String signData = "vnp_Amount=200000000&vnp_TxnRef=PAY_103";
        params.put("vnp_SecureHash", HmacSha512Util.hmacSHA512("secret_key_for_test", signData));

        VnPayCallbackDto dto = new VnPayCallbackDto();
        dto.setVnp_TxnRef("PAY_103");
        dto.setVnp_Amount("200000000");
        dto.setAllParams(params);

        when(paymentRepository.findByPaymentReferenceForUpdate("PAY_103")).thenReturn(Optional.of(payment));

        VnPayIpnResponse response = paymentService.execute(dto);

        assertThat(response.getRspCode()).isEqualTo("04");
        assertThat(response.getMessage()).isEqualTo("Invalid amount");
        verify(paymentBookingConfirmPort, never()).confirmBooking(any(), any());
    }

    @Test
    void handleCallback_whenBankReportsFailed_shouldMarkFailedAndReturn00() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_104")
                .bookingId(504L)
                .amount(BigDecimal.valueOf(1000000))
                .status(PaymentStatus.PENDING)
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_TxnRef", "PAY_104");
        params.put("vnp_ResponseCode", "24"); // Cancelled by customer
        String signData = "vnp_Amount=100000000&vnp_ResponseCode=24&vnp_TxnRef=PAY_104";
        params.put("vnp_SecureHash", HmacSha512Util.hmacSHA512("secret_key_for_test", signData));

        VnPayCallbackDto dto = new VnPayCallbackDto();
        dto.setVnp_TxnRef("PAY_104");
        dto.setVnp_Amount("100000000");
        dto.setVnp_ResponseCode("24");
        dto.setVnp_TransactionStatus("02");
        dto.setAllParams(params);

        when(paymentRepository.findByPaymentReferenceForUpdate("PAY_104")).thenReturn(Optional.of(payment));

        VnPayIpnResponse response = paymentService.execute(dto);

        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
        verify(paymentBookingConfirmPort, never()).confirmBooking(any(), any());
    }

    @Test
    void handleCallback_whenAlreadyConfirmedByOtherPayment_shouldMarkReconciliationAndReturn00() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_105")
                .bookingId(505L)
                .amount(BigDecimal.valueOf(1000000))
                .status(PaymentStatus.PENDING)
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_TxnRef", "PAY_105");
        params.put("vnp_ResponseCode", "00");
        String signData = "vnp_Amount=100000000&vnp_ResponseCode=00&vnp_TxnRef=PAY_105";
        params.put("vnp_SecureHash", HmacSha512Util.hmacSHA512("secret_key_for_test", signData));

        VnPayCallbackDto dto = new VnPayCallbackDto();
        dto.setVnp_TxnRef("PAY_105");
        dto.setVnp_Amount("100000000");
        dto.setVnp_ResponseCode("00");
        dto.setVnp_TransactionStatus("00");
        dto.setVnp_TransactionNo("TXN_DUPLICATE_99");
        dto.setAllParams(params);

        when(paymentRepository.findByPaymentReferenceForUpdate("PAY_105")).thenReturn(Optional.of(payment));
        when(paymentBookingConfirmPort.confirmBooking(eq(505L), any(LocalDateTime.class)))
                .thenReturn(PaymentBookingConfirmPort.BookingConfirmResult.ALREADY_CONFIRMED);

        VnPayIpnResponse response = paymentService.execute(dto);

        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.isReconciliationRequired()).isTrue();
        assertThat(payment.getRefundReason()).isEqualTo("DUPLICATE_PAYMENT_FOR_CONFIRMED_BOOKING");
        verify(paymentRepository).save(payment);
    }

    @Test
    void handleCallback_whenAlreadyRefunded_shouldReturnAlreadyConfirmed() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_106")
                .bookingId(506L)
                .amount(BigDecimal.valueOf(1000000))
                .status(PaymentStatus.REFUNDED)
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_TxnRef", "PAY_106");
        String signData = "vnp_Amount=100000000&vnp_TxnRef=PAY_106";
        params.put("vnp_SecureHash", HmacSha512Util.hmacSHA512("secret_key_for_test", signData));

        VnPayCallbackDto dto = new VnPayCallbackDto();
        dto.setVnp_TxnRef("PAY_106");
        dto.setVnp_Amount("100000000");
        dto.setAllParams(params);

        when(paymentRepository.findByPaymentReferenceForUpdate("PAY_106")).thenReturn(Optional.of(payment));

        VnPayIpnResponse response = paymentService.execute(dto);

        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(response.getMessage()).isEqualTo("Order already confirmed");
        verify(paymentBookingConfirmPort, never()).confirmBooking(any(), any());
    }

    @Test
    void createPayment_whenPendingPaymentStillValid_shouldReuseRedirectUrl() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingReference("BKG_REUSE_01")
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        PaymentBookingPort.BookingSnapshot snapshot = new PaymentBookingPort.BookingSnapshot(
                200L, "BKG_REUSE_01", 10L, BigDecimal.valueOf(1000000), "HELD", expiresAt
        );
        Payment existingPending = Payment.builder()
                .id(999L)
                .paymentReference("PAY_REUSE_EXISTING")
                .bookingId(200L)
                .amount(BigDecimal.valueOf(1000000))
                .status(PaymentStatus.PENDING)
                .build();
        existingPending.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        when(paymentIdentityPort.findActiveUserIdByUsername("customer1")).thenReturn(Optional.of(10L));
        when(paymentBookingPort.findBookingByReference("BKG_REUSE_01")).thenReturn(Optional.of(snapshot));
        when(paymentRepository.findFirstByBookingIdAndStatus(200L, PaymentStatus.SUCCESS)).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBookingIdAndStatus(200L, PaymentStatus.PENDING)).thenReturn(Optional.of(existingPending));

        PaymentInitResponse response = paymentService.execute(command, "customer1", "127.0.0.1");

        assertThat(response.getPaymentReference()).isEqualTo("PAY_REUSE_EXISTING");
        assertThat(response.getRedirectPaymentUrl()).contains("PAY_REUSE_EXISTING");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPayment_whenAccountLocked_shouldThrowForbiddenException() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingReference("BKG_LOCKED_01")
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        when(paymentIdentityPort.findActiveUserIdByUsername("locked_user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.execute(command, "locked_user", "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void createPayment_whenBookingAlreadyPaid_shouldThrowBusinessException() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingReference("BKG_PAID_01")
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        PaymentBookingPort.BookingSnapshot snapshot = new PaymentBookingPort.BookingSnapshot(
                300L, "BKG_PAID_01", 10L, BigDecimal.valueOf(1000000), "CONFIRMED", LocalDateTime.now().plusMinutes(5)
        );
        Payment successfulPayment = Payment.builder().paymentReference("PAY_DONE").status(PaymentStatus.SUCCESS).build();

        when(paymentIdentityPort.findActiveUserIdByUsername("customer1")).thenReturn(Optional.of(10L));
        when(paymentBookingPort.findBookingByReference("BKG_PAID_01")).thenReturn(Optional.of(snapshot));
        when(paymentRepository.findFirstByBookingIdAndStatus(300L, PaymentStatus.SUCCESS)).thenReturn(Optional.of(successfulPayment));

        assertThatThrownBy(() -> paymentService.execute(command, "customer1", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("BOOKING_ALREADY_PAID");
    }

    @Test
    void createPayment_whenPendingExpired_shouldMarkFailedAndCreateNewPayment() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingReference("BKG_EXP_PENDING")
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        PaymentBookingPort.BookingSnapshot snapshot = new PaymentBookingPort.BookingSnapshot(
                205L, "BKG_EXP_PENDING", 10L, BigDecimal.valueOf(1000000), "HELD", LocalDateTime.now().plusMinutes(5)
        );
        Payment stalePending = Payment.builder()
                .id(998L)
                .paymentReference("PAY_STALE_PENDING")
                .bookingId(205L)
                .amount(BigDecimal.valueOf(1000000))
                .status(PaymentStatus.PENDING)
                .build();
        stalePending.setCreatedAt(LocalDateTime.now().minusMinutes(15)); // Expired >10 mins ago

        when(paymentIdentityPort.findActiveUserIdByUsername("customer1")).thenReturn(Optional.of(10L));
        when(paymentBookingPort.findBookingByReference("BKG_EXP_PENDING")).thenReturn(Optional.of(snapshot));
        when(paymentRepository.findFirstByBookingIdAndStatus(205L, PaymentStatus.SUCCESS)).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByBookingIdAndStatus(205L, PaymentStatus.PENDING)).thenReturn(Optional.of(stalePending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentInitResponse response = paymentService.execute(command, "customer1", "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(stalePending.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository, times(2)).save(any(Payment.class)); // 1 to fail stale, 1 to save new pending
    }

    @Test
    void handleCallback_whenStatusFailedAndBankReportsSuccess_shouldMarkReconciliationAndReturn00() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_FAILED_BEFORE")
                .bookingId(507L)
                .amount(BigDecimal.valueOf(1000000))
                .status(PaymentStatus.FAILED)
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "100000000");
        params.put("vnp_TxnRef", "PAY_FAILED_BEFORE");
        params.put("vnp_ResponseCode", "00");
        String signData = "vnp_Amount=100000000&vnp_ResponseCode=00&vnp_TxnRef=PAY_FAILED_BEFORE";
        params.put("vnp_SecureHash", HmacSha512Util.hmacSHA512("secret_key_for_test", signData));

        VnPayCallbackDto dto = new VnPayCallbackDto();
        dto.setVnp_TxnRef("PAY_FAILED_BEFORE");
        dto.setVnp_Amount("100000000");
        dto.setVnp_ResponseCode("00");
        dto.setVnp_TransactionStatus("00");
        dto.setVnp_TransactionNo("TXN_LATE_SUCCESS");
        dto.setAllParams(params);

        when(paymentRepository.findByPaymentReferenceForUpdate("PAY_FAILED_BEFORE")).thenReturn(Optional.of(payment));

        VnPayIpnResponse response = paymentService.execute(dto);

        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.isReconciliationRequired()).isTrue();
        assertThat(payment.getRefundReason()).isEqualTo("SUCCESS_CALLBACK_AFTER_FAILED_STATUS");
        verify(paymentRepository).save(payment);
    }

    @Test
    void createPayment_whenHoldBoundary59Seconds_shouldThrowBookingExpiringSoon() {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingReference("BKG_EXP_59S")
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        PaymentBookingPort.BookingSnapshot snapshot = new PaymentBookingPort.BookingSnapshot(
                206L, "BKG_EXP_59S", 10L, BigDecimal.valueOf(1000000), "HELD", LocalDateTime.now().plusSeconds(59)
        );

        when(paymentIdentityPort.findActiveUserIdByUsername("customer1")).thenReturn(Optional.of(10L));
        when(paymentBookingPort.findBookingByReference("BKG_EXP_59S")).thenReturn(Optional.of(snapshot));

        assertThatThrownBy(() -> paymentService.execute(command, "customer1", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("BOOKING_EXPIRING_SOON");
    }
}
