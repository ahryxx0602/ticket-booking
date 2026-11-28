package com.ticket.concurrency;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.ticket.TicketApplication;
import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.HoldSeatCommand;
import com.ticket.booking.application.dto.TicketResponse;
import com.ticket.booking.application.usecase.GetMyBookingsUseCase;
import com.ticket.booking.application.usecase.GetTicketUseCase;
import com.ticket.booking.application.usecase.HoldSeatUseCase;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.notification.infrastructure.mail.QrCodeUtil;
import com.ticket.payment.application.dto.CreatePaymentCommand;
import com.ticket.payment.application.dto.CustomerPaymentStatusResponse;
import com.ticket.payment.application.dto.PaymentInitResponse;
import com.ticket.payment.application.usecase.CreatePaymentUseCase;
import com.ticket.payment.application.usecase.GetCustomerPaymentStatusUseCase;
import com.ticket.payment.application.usecase.HandleVnPayBrowserReturnUseCase;
import com.ticket.payment.infrastructure.config.VnPayProperties;
import com.ticket.payment.infrastructure.crypto.HmacSha512Util;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.ForbiddenException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TicketApplication.class)
@ActiveProfiles("test")
@DisplayName("Gate B5.3-E: Backend Capabilities Verification Suite")
class GateB53CapabilityIT {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private HoldSeatUseCase holdSeatUseCase;
    @Autowired private GetMyBookingsUseCase getMyBookingsUseCase;
    @Autowired private GetTicketUseCase getTicketUseCase;
    @Autowired private CreatePaymentUseCase createPaymentUseCase;
    @Autowired private GetCustomerPaymentStatusUseCase getCustomerPaymentStatusUseCase;
    @Autowired private HandleVnPayBrowserReturnUseCase handleVnPayBrowserReturnUseCase;
    @Autowired private VnPayProperties vnPayProperties;
    @Autowired private QrCodeUtil qrCodeUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long testTripId;
    private Long customer1Id;
    private String customer1Username;
    private Long customer2Id;
    private String customer2Username;

    @BeforeAll
    static void checkInfrastructureReachable() {
        String mysqlHost = System.getenv().getOrDefault("MYSQL_HOST", "127.0.0.1");
        int mysqlPort = Integer.parseInt(System.getenv().getOrDefault("MYSQL_PORT", "3307"));
        boolean mysqlReachable = false;
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(mysqlHost, mysqlPort), 1500);
            mysqlReachable = true;
        } catch (Exception ignored) {}

        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
        int redisPort = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        boolean redisReachable = false;
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(redisHost, redisPort), 1500);
            redisReachable = true;
        } catch (Exception ignored) {}

        Assumptions.assumeTrue(mysqlReachable, "MySQL server must be reachable");
        Assumptions.assumeTrue(redisReachable, "Redis server must be reachable");
    }

    @BeforeEach
    void setUpFixture() {
        customer1Username = "cap_cust1_" + UUID.randomUUID().toString().substring(0, 8);
        User u1 = User.builder()
                .username(customer1Username)
                .email(customer1Username + "@velobus.vn")
                .fullName("Capability Customer 1")
                .phone("0981111222")
                .passwordHash(passwordEncoder.encode("Test_Pass_123"))
                .role(UserRole.ROLE_CUSTOMER)
                .customRoleCode("ROLE_CUSTOMER")
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .build();
        User savedU1 = userRepository.save(u1);
        customer1Id = savedU1.getId();

        customer2Username = "cap_cust2_" + UUID.randomUUID().toString().substring(0, 8);
        User u2 = User.builder()
                .username(customer2Username)
                .email(customer2Username + "@velobus.vn")
                .fullName("Capability Customer 2")
                .phone("0983333444")
                .passwordHash(passwordEncoder.encode("Test_Pass_456"))
                .role(UserRole.ROLE_CUSTOMER)
                .customRoleCode("ROLE_CUSTOMER")
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .build();
        User savedU2 = userRepository.save(u2);
        customer2Id = savedU2.getId();

        String stCode1 = "D" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        String stCode2 = "A" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        jdbcTemplate.update("INSERT INTO stations (code, name, city) VALUES (?, ?, ?)", stCode1, "Dep " + stCode1, "HN");
        Long depId = jdbcTemplate.queryForObject("SELECT id FROM stations WHERE code = ?", Long.class, stCode1);
        jdbcTemplate.update("INSERT INTO stations (code, name, city) VALUES (?, ?, ?)", stCode2, "Arr " + stCode2, "SG");
        Long arrId = jdbcTemplate.queryForObject("SELECT id FROM stations WHERE code = ?", Long.class, stCode2);

        String tripCode = "TRP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        jdbcTemplate.update("""
                INSERT INTO trips (trip_code, departure_station_id, arrival_station_id, departure_time, arrival_time,
                                   transport_type, total_seats, base_price, status, settlement_status)
                VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY),
                        'BUS', 12, 250000.00, 'SCHEDULED', 'UNSETTLED')
                """, tripCode, depId, arrId);
        testTripId = jdbcTemplate.queryForObject("SELECT id FROM trips WHERE trip_code = ?", Long.class, tripCode);

        for (String seatNum : List.of("11A", "11B", "11C", "11D", "12A", "12B", "12C", "12D")) {
            jdbcTemplate.update("""
                    INSERT INTO seats (trip_id, seat_number, seat_type, price_multiplier)
                    VALUES (?, ?, 'STANDARD', 1.00)
                    """, testTripId, seatNum);
        }
    }

    @Test
    @DisplayName("Cap 01: VNPay Browser Return - Valid Signature redirects to frontend with responseCode and bookingRef")
    void cap01_vnpayBrowserReturnValidSignature() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(testTripId);
        cmd.setSeatNumbers(List.of("11A"));
        cmd.setClientRequestId(UUID.randomUUID().toString());
        BookingResponse booking = holdSeatUseCase.execute(cmd, customer1Username);

        CreatePaymentCommand payCmd = new CreatePaymentCommand();
        payCmd.setBookingReference(booking.getBookingReference());
        payCmd.setPaymentMethod(com.ticket.payment.domain.model.PaymentMethod.VNPAY);
        PaymentInitResponse payInit = createPaymentUseCase.execute(payCmd, customer1Username, "127.0.0.1");

        Map<String, String> returnParams = new HashMap<>();
        returnParams.put("vnp_TxnRef", payInit.getPaymentReference());
        returnParams.put("vnp_Amount", "25000000");
        returnParams.put("vnp_ResponseCode", "00");
        returnParams.put("vnp_TransactionNo", "14888999");

        List<String> fieldNames = new ArrayList<>(returnParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String f : fieldNames) {
            if (!hashData.isEmpty()) hashData.append('&');
            hashData.append(f).append('=').append(URLEncoder.encode(returnParams.get(f), StandardCharsets.US_ASCII));
        }
        String secureHash = HmacSha512Util.hmacSHA512(vnPayProperties.getHashSecret(), hashData.toString());
        returnParams.put("vnp_SecureHash", secureHash);

        String redirectUrl = handleVnPayBrowserReturnUseCase.execute(returnParams);

        assertThat(redirectUrl).contains(booking.getBookingReference());
        assertThat(redirectUrl).contains("vnp_ResponseCode=00");
        assertThat(redirectUrl).contains("vnp_TxnRef=" + payInit.getPaymentReference());
        assertThat(redirectUrl).doesNotContain("vnp_Error=INVALID_SIGNATURE");
    }

    @Test
    @DisplayName("Cap 01b: VNPay Browser Return - Tampered Signature redirects with INVALID_SIGNATURE error without booking disclose")
    void cap01b_vnpayBrowserReturnTamperedSignature() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(testTripId);
        cmd.setSeatNumbers(List.of("11B"));
        cmd.setClientRequestId(UUID.randomUUID().toString());
        BookingResponse booking = holdSeatUseCase.execute(cmd, customer1Username);

        CreatePaymentCommand payCmd = new CreatePaymentCommand();
        payCmd.setBookingReference(booking.getBookingReference());
        payCmd.setPaymentMethod(com.ticket.payment.domain.model.PaymentMethod.VNPAY);
        PaymentInitResponse payInit = createPaymentUseCase.execute(payCmd, customer1Username, "127.0.0.1");

        Map<String, String> returnParams = new HashMap<>();
        returnParams.put("vnp_TxnRef", payInit.getPaymentReference());
        returnParams.put("vnp_ResponseCode", "00");
        returnParams.put("vnp_SecureHash", "tampered_hash_12345");

        String redirectUrl = handleVnPayBrowserReturnUseCase.execute(returnParams);

        // BLOCKER-06: MUST NOT reveal bookingReference when signature is invalid
        assertThat(redirectUrl).contains("vnp_Error=INVALID_SIGNATURE");
        assertThat(redirectUrl).doesNotContain(booking.getBookingReference());
    }

    @Test
    @DisplayName("Cap 02: Customer Payment Status Query - Verifies ownership and payment lifecycle")
    void cap02_customerPaymentStatusQuery() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(testTripId);
        cmd.setSeatNumbers(List.of("11C"));
        cmd.setClientRequestId(UUID.randomUUID().toString());
        BookingResponse booking = holdSeatUseCase.execute(cmd, customer1Username);

        // Before payment initiation: hasPaymentAttempt is false
        CustomerPaymentStatusResponse before = getCustomerPaymentStatusUseCase.execute(booking.getBookingReference(), customer1Username);
        assertThat(before.isHasPaymentAttempt()).isFalse();
        assertThat(before.getBookingStatus()).isEqualTo("HELD");
        assertThat(before.isCanAttemptPayment()).isTrue();

        // Initiate payment
        CreatePaymentCommand payCmd = new CreatePaymentCommand();
        payCmd.setBookingReference(booking.getBookingReference());
        payCmd.setPaymentMethod(com.ticket.payment.domain.model.PaymentMethod.VNPAY);
        PaymentInitResponse payInit = createPaymentUseCase.execute(payCmd, customer1Username, "127.0.0.1");

        // After payment initiation: hasPaymentAttempt is true, status PENDING
        CustomerPaymentStatusResponse after = getCustomerPaymentStatusUseCase.execute(booking.getBookingReference(), customer1Username);
        assertThat(after.isHasPaymentAttempt()).isTrue();
        assertThat(after.getPaymentReference()).isEqualTo(payInit.getPaymentReference());
        assertThat(after.getPaymentStatus().name()).isEqualTo("PENDING");
        assertThat(after.getTotalAttempts()).isEqualTo(1);

        // Access denied for other customer
        assertThatThrownBy(() -> getCustomerPaymentStatusUseCase.execute(booking.getBookingReference(), customer2Username))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Cap 02b: Multiple Payment Attempts Aggregation - Preserves older reconciliation issues")
    void cap02b_multiplePaymentAttemptsAggregation() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(testTripId);
        cmd.setSeatNumbers(List.of("11D"));
        cmd.setClientRequestId(UUID.randomUUID().toString());
        BookingResponse booking = holdSeatUseCase.execute(cmd, customer1Username);

        Long bookingId = jdbcTemplate.queryForObject("SELECT id FROM bookings WHERE booking_reference = ?", Long.class, booking.getBookingReference());

        // Attempt 1: Failed with reconciliation_required = true
        String payRef1 = "PAY-ATTEMPT1-" + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update("""
                INSERT INTO payments (payment_reference, booking_id, amount, currency, payment_method, status, reconciliation_required, created_at)
                VALUES (?, ?, 250000.00, 'VND', 'VNPAY', 'FAILED', 1, NOW() - INTERVAL 10 MINUTE)
                """, payRef1, bookingId);

        // Attempt 2: Current pending attempt
        String payRef2 = "PAY-ATTEMPT2-" + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update("""
                INSERT INTO payments (payment_reference, booking_id, amount, currency, payment_method, status, reconciliation_required, created_at)
                VALUES (?, ?, 250000.00, 'VND', 'VNPAY', 'PENDING', 0, NOW())
                """, payRef2, bookingId);

        CustomerPaymentStatusResponse status = getCustomerPaymentStatusUseCase.execute(booking.getBookingReference(), customer1Username);

        assertThat(status.getTotalAttempts()).isEqualTo(2);
        // BLOCKER-02: Older reconciliation issue MUST NOT be hidden by newer attempt!
        assertThat(status.isReconciliationRequired()).isTrue();
        assertThat(status.getPaymentReference()).isEqualTo(payRef2);
        assertThat(status.getPaymentStatus().name()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Cap 03: Electronic Ticket Retrieval - Strictly requires CONFIRMED status and generates signed QR")
    void cap03_electronicTicketRetrieval() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(testTripId);
        cmd.setSeatNumbers(List.of("12A"));
        cmd.setClientRequestId(UUID.randomUUID().toString());
        BookingResponse booking = holdSeatUseCase.execute(cmd, customer1Username);

        // While HELD: ticket retrieval is rejected
        assertThatThrownBy(() -> getTicketUseCase.getTicket(booking.getBookingReference(), customer1Username))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("BOOKING_NOT_ELIGIBLE_FOR_TICKET");

        // Update booking status to CONFIRMED in DB
        jdbcTemplate.update("UPDATE bookings SET status = 'CONFIRMED' WHERE booking_reference = ?", booking.getBookingReference());

        // Now ticket retrieval succeeds with authoritative signed QR payload and Base64 image
        TicketResponse ticket = getTicketUseCase.getTicket(booking.getBookingReference(), customer1Username);
        assertThat(ticket.getBookingReference()).isEqualTo(booking.getBookingReference());
        assertThat(ticket.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(ticket.getQrPayload()).startsWith("v1|" + booking.getBookingReference() + "|" + testTripId + "|");
        assertThat(ticket.getQrImageBase64()).isNotEmpty();

        // Customer 2 cannot access Customer 1's ticket
        assertThatThrownBy(() -> getTicketUseCase.getTicket(booking.getBookingReference(), customer2Username))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Cap 03b: QR Generation, ZXing Decodability & HMAC Verification")
    void cap03b_qrGenerationAndZxingDecodingVerification() throws Exception {
        String testBookingRef = "BKG-ZXING-TEST-" + UUID.randomUUID().toString().substring(0, 6);
        Long testTrip = 9999L;
        String isolatedSecret = "test-only-signing-secret-key-32-bytes!";

        String payload = qrCodeUtil.buildQrPayload(testBookingRef, testTrip, isolatedSecret);
        assertThat(payload).startsWith("v1|" + testBookingRef + "|" + testTrip + "|");

        String qrBase64 = qrCodeUtil.generateQrCodeBase64(payload);
        assertThat(qrBase64).isNotEmpty();

        // Decode Base64 to image and decode with ZXing
        byte[] pngBytes = Base64.getDecoder().decode(qrBase64);
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(pngBytes));
        assertThat(bufferedImage).isNotNull();

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage)));
        Result zxingResult = new MultiFormatReader().decode(bitmap);

        // Assert decoded content equals issued payload
        assertThat(zxingResult.getText()).isEqualTo(payload);

        // Verify HMAC signature
        String[] parts = payload.split("\\|");
        assertThat(parts).hasSize(4);
        String signature = parts[3];

        boolean valid = qrCodeUtil.verifyHmacSignature(testBookingRef, testTrip, signature, isolatedSecret);
        assertThat(valid).isTrue();

        boolean tampered = qrCodeUtil.verifyHmacSignature(testBookingRef, testTrip, "tampered-signature", isolatedSecret);
        assertThat(tampered).isFalse();
    }

    @Test
    @DisplayName("Cap 04: Server-side My Bookings Status Filtering and Pagination")
    void cap04_serverSideMyBookingsFiltering() {
        HoldSeatCommand cmd1 = new HoldSeatCommand();
        cmd1.setTripId(testTripId);
        cmd1.setSeatNumbers(List.of("12B"));
        cmd1.setClientRequestId(UUID.randomUUID().toString());
        BookingResponse b1 = holdSeatUseCase.execute(cmd1, customer1Username);

        HoldSeatCommand cmd2 = new HoldSeatCommand();
        cmd2.setTripId(testTripId);
        cmd2.setSeatNumbers(List.of("12C"));
        cmd2.setClientRequestId(UUID.randomUUID().toString());
        BookingResponse b2 = holdSeatUseCase.execute(cmd2, customer1Username);
        jdbcTemplate.update("UPDATE bookings SET status = 'CONFIRMED' WHERE booking_reference = ?", b2.getBookingReference());

        // Query ALL
        PageResponse<BookingResponse> allPage = getMyBookingsUseCase.execute(customer1Username, 0, 10, "ALL");
        assertThat(allPage.getItems()).hasSizeGreaterThanOrEqualTo(2);

        // Query HELD only
        PageResponse<BookingResponse> heldPage = getMyBookingsUseCase.execute(customer1Username, 0, 10, "HELD");
        assertThat(heldPage.getItems()).allMatch(b -> b.getStatus() == BookingStatus.HELD);
        assertThat(heldPage.getItems()).extracting(BookingResponse::getBookingReference).contains(b1.getBookingReference());

        // Query CONFIRMED only
        PageResponse<BookingResponse> confPage = getMyBookingsUseCase.execute(customer1Username, 0, 10, "CONFIRMED");
        assertThat(confPage.getItems()).allMatch(b -> b.getStatus() == BookingStatus.CONFIRMED);
        assertThat(confPage.getItems()).extracting(BookingResponse::getBookingReference).contains(b2.getBookingReference());
    }
}
