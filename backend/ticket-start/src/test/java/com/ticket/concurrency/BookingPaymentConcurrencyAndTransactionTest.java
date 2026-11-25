package com.ticket.concurrency;

import com.ticket.adapter.PaymentBookingConfirmPortAdapter;
import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.HoldSeatCommand;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingItem;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.port.CatalogTripPort;
import com.ticket.booking.domain.port.IdentityUserPort;
import com.ticket.booking.domain.port.PaymentRefundPort;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.booking.domain.repository.SeatLockRepository;
import com.ticket.booking.infrastructure.service.BookingCommandServiceImpl;
import com.ticket.payment.application.dto.CreatePaymentCommand;
import com.ticket.payment.application.dto.PaymentInitResponse;
import com.ticket.payment.application.dto.VnPayCallbackDto;
import com.ticket.payment.application.dto.VnPayIpnResponse;
import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.domain.port.PaymentBookingPort;
import com.ticket.payment.domain.port.PaymentIdentityPort;
import com.ticket.payment.domain.repository.PaymentRepository;
import com.ticket.payment.infrastructure.config.VnPayProperties;
import com.ticket.payment.infrastructure.crypto.HmacSha512Util;
import com.ticket.payment.infrastructure.service.PaymentServiceImpl;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * QA-001, BE-CON-001, BE-PAY-001, BE-TXN-001:
 * Kiểm thử đa luồng (Multi-Thread Concurrency) và Transaction Rollback-Only Boundary.
 */
@DisplayName("In-Memory Concurrency & Transaction Simulation Unit Suite (Supplemented by RealMySqlConcurrencyAndTransactionIT)")
class BookingPaymentConcurrencyAndTransactionTest {

    @Test
    @DisplayName("BE-CON-001: When Redis lock is bypassed/evicted, DB PESSIMISTIC_WRITE seat lock + Current Read must serialize 10 concurrent threads so only 1 succeeds")
    void concurrentHoldSeat_WhenRedisBypassed_DbSeatRowLockAndCurrentReadMustPreventDoubleBooking() throws Exception {
        final int threadCount = 10;
        final ReentrantLock dbSeatRowLock = new ReentrantLock(true);
        final Set<String> committedBookedSeats = new HashSet<>();
        final AtomicInteger idSeq = new AtomicInteger(1);

        // Simulate Redis eviction/failover where acquireLocks always returns true for every concurrent thread
        SeatLockRepository bypassedRedisLockRepo = new SeatLockRepository() {
            @Override
            public boolean acquireLocks(Long tripId, List<String> seatNumbers, String ownerToken, Duration ttl) {
                return true;
            }

            @Override
            public void releaseOwnedLocks(Long tripId, List<String> seatNumbers, String ownerToken) {
            }

            @Override
            public boolean isSeatLocked(Long tripId, String seatNumber) {
                return false;
            }
        };

        // Simulate DB row-level X-lock on SeatJpaEntity held until transaction commits/rolls back
        ThreadLocal<Boolean> holdsDbSeatLock = ThreadLocal.withInitial(() -> false);
        PlatformTransactionManager txManager = new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() throws TransactionException {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
                if (Boolean.TRUE.equals(holdsDbSeatLock.get())) {
                    holdsDbSeatLock.set(false);
                    dbSeatRowLock.unlock();
                }
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
                if (Boolean.TRUE.equals(holdsDbSeatLock.get())) {
                    holdsDbSeatLock.set(false);
                    dbSeatRowLock.unlock();
                }
            }
        };

        CatalogTripPort catalogTripPort = new CatalogTripPort() {
            @Override
            public Optional<TripSnapshot> findTripWithSeats(Long tripId) {
                return Optional.of(new TripSnapshot(
                        1L,
                        true,
                        Map.of("01A", new TripSeatInfo(101L, "01A", BigDecimal.valueOf(500000)))
                ));
            }

            @Override
            public int lockSeatsForBooking(Long tripId, List<String> sortedSeatNumbers) {
                dbSeatRowLock.lock();
                holdsDbSeatLock.set(true);
                return sortedSeatNumbers.size();
            }
        };

        BookingRepository bookingRepository = new BookingRepository() {
            @Override
            public Optional<Booking> findById(Long id) { return Optional.empty(); }
            @Override
            public Optional<Booking> findByReference(String bookingReference) { return Optional.empty(); }
            @Override
            public Optional<Booking> findByReferenceForUpdate(String bookingReference) { return Optional.empty(); }

            @Override
            public Set<String> findActiveBookedSeatNumbers(Long tripId, LocalDateTime now) {
                // Current Read (@Lock(PESSIMISTIC_WRITE)) sees the latest committed seats after acquiring seat lock
                return new HashSet<>(committedBookedSeats);
            }

            @Override
            public PageResponse<Booking> findByUserId(Long userId, int page, int size) { return null; }
            @Override
            public PageResponse<Booking> findAllWithFilters(BookingStatus status, Long tripId, Long userId, int page, int size) { return null; }
            @Override
            public List<Booking> findExpiredHeldBookings(LocalDateTime now, int limit) { return List.of(); }

            @Override
            public Booking save(Booking booking) {
                booking.setId((long) idSeq.getAndIncrement());
                for (BookingItem item : booking.getItems()) {
                    committedBookedSeats.add(item.getSeatNumber());
                }
                return booking;
            }
        };

        IdentityUserPort identityUserPort = username ->
                Optional.of(new IdentityUserPort.AuthenticatedUserInfo(10L, "0901234567", false));

        BookingCommandServiceImpl bookingService = new BookingCommandServiceImpl(
                bookingRepository,
                bypassedRedisLockRepo,
                catalogTripPort,
                identityUserPort,
                mock(PaymentRefundPort.class),
                new TransactionTemplate(txManager),
                mock(ApplicationEventPublisher.class)
        );

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                startGate.await(5, TimeUnit.SECONDS);
                HoldSeatCommand cmd = new HoldSeatCommand();
                cmd.setTripId(1L);
                cmd.setSeatNumbers(List.of("01A"));
                try {
                    BookingResponse resp = bookingService.execute(cmd, "customer1");
                    return resp != null && resp.getStatus() == BookingStatus.HELD;
                } catch (BusinessException ex) {
                    if ("SEAT_ALREADY_RESERVED".equals(ex.getCode())) {
                        return false;
                    }
                    throw ex;
                }
            }));
        }

        startGate.countDown();
        int successCount = 0;
        int rejectedCount = 0;
        for (Future<Boolean> f : futures) {
            if (Boolean.TRUE.equals(f.get(10, TimeUnit.SECONDS))) {
                successCount++;
            } else {
                rejectedCount++;
            }
        }
        pool.shutdownNow();

        assertThat(successCount).isEqualTo(1);
        assertThat(rejectedCount).isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("BE-PAY-001: Concurrent checkout requests for the same booking must be serialized by findBookingByReferenceForUpdate and return the exact same PENDING paymentReference")
    void concurrentCreatePayment_ForSameBooking_ShouldCreateSinglePendingRecordAndReturnSameReference() throws Exception {
        final int threadCount = 5;
        final ReentrantLock bookingRowLock = new ReentrantLock(true);
        final List<Payment> persistedPayments = Collections.synchronizedList(new ArrayList<>());

        PaymentIdentityPort identityPort = username -> Optional.of(10L);
        PaymentBookingPort bookingPort = new PaymentBookingPort() {
            private final BookingSnapshot snapshot = new BookingSnapshot(
                    500L, "BKG-CONCURRENT-500", 10L, BigDecimal.valueOf(800000), "HELD", LocalDateTime.now().plusMinutes(8)
            );

            @Override
            public Optional<BookingSnapshot> findBookingByReference(String bookingReference) {
                return Optional.of(snapshot);
            }

            @Override
            public Optional<BookingSnapshot> findBookingByReferenceForUpdate(String bookingReference) {
                bookingRowLock.lock();
                return Optional.of(snapshot);
            }

            @Override
            public Optional<BookingSnapshot> findBookingById(Long bookingId) {
                return Optional.of(snapshot);
            }
        };

        PaymentRepository paymentRepository = new PaymentRepository() {
            @Override
            public Optional<Payment> findById(Long id) { return Optional.empty(); }
            @Override
            public Optional<Payment> findByPaymentReference(String paymentReference) { return Optional.empty(); }
            @Override
            public Optional<Payment> findByPaymentReferenceForUpdate(String paymentReference) { return Optional.empty(); }
            @Override
            public Optional<Payment> findFirstByBookingIdAndStatusOrderByIdDesc(Long bookingId, PaymentStatus status) {
                return findFirstByBookingIdAndStatus(bookingId, status);
            }

            @Override
            public Optional<Payment> findFirstByBookingIdAndStatus(Long bookingId, PaymentStatus status) {
                synchronized (persistedPayments) {
                    return persistedPayments.stream()
                            .filter(p -> p.getBookingId().equals(bookingId) && p.getStatus() == status)
                            .findFirst();
                }
            }

            @Override
            public com.ticket.payment.domain.model.PaymentRevenueReport getRevenueReport(LocalDateTime from, LocalDateTime to) { return null; }
            @Override
            public PageResponse<Payment> findPaymentsWithFilters(PaymentStatus status, PaymentMethod paymentMethod, String paymentReference, Boolean reconciliationRequired, int page, int size) { return null; }

            @Override
            public Payment save(Payment payment) {
                synchronized (persistedPayments) {
                    if (payment.getCreatedAt() == null) {
                        payment.setCreatedAt(LocalDateTime.now());
                    }
                    persistedPayments.add(payment);
                    return payment;
                }
            }
        };

        VnPayProperties props = new VnPayProperties();
        props.setHashSecret("secret_key_for_concurrency_test_32bytes");
        props.setTmnCode("TMN_TEST");
        props.setPayUrl("https://sandbox.vnpayment.vn/pay");
        props.setReturnUrl("http://localhost:8080/return");

        PaymentServiceImpl paymentService = new PaymentServiceImpl(
                paymentRepository,
                bookingPort,
                mock(com.ticket.payment.domain.port.PaymentBookingConfirmPort.class),
                identityPort,
                props,
                mock(ApplicationEventPublisher.class)
        );

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                startGate.await(5, TimeUnit.SECONDS);
                CreatePaymentCommand cmd = CreatePaymentCommand.builder()
                        .bookingReference("BKG-CONCURRENT-500")
                        .paymentMethod(PaymentMethod.VNPAY)
                        .build();
                try {
                    PaymentInitResponse resp = paymentService.execute(cmd, "customer1", "127.0.0.1");
                    return resp.getPaymentReference();
                } finally {
                    if (bookingRowLock.isHeldByCurrentThread()) {
                        bookingRowLock.unlock();
                    }
                }
            }));
        }

        startGate.countDown();
        Set<String> returnedReferences = ConcurrentHashMap.newKeySet();
        for (Future<String> f : futures) {
            returnedReferences.add(f.get(10, TimeUnit.SECONDS));
        }
        pool.shutdownNow();

        assertThat(persistedPayments).hasSize(1);
        assertThat(returnedReferences).hasSize(1);
    }

    @Test
    @DisplayName("BE-TXN-001: When booking is EXPIRED during VNPay IPN callback, shared transaction must NOT be marked rollback-only and must persist reconciliationRequired=true cleanly")
    void handleIpnCallback_WhenBookingAlreadyExpired_ShouldNotMarkSharedTransactionRollbackOnly() {
        AtomicBoolean transactionMarkedRollbackOnly = new AtomicBoolean(false);

        PlatformTransactionManager strictTxManager = new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() throws TransactionException {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
            }

            @Override
            protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
                transactionMarkedRollbackOnly.set(true);
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
                transactionMarkedRollbackOnly.set(true);
            }
        };

        Booking expiredBooking = Booking.builder()
                .id(777L)
                .bookingReference("BKG-EXPIRED-777")
                .userId(10L)
                .tripId(1L)
                .status(BookingStatus.EXPIRED)
                .totalAmount(BigDecimal.valueOf(600000))
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        BookingRepository bookingRepository = new BookingRepository() {
            @Override
            public Optional<Booking> findById(Long id) { return Optional.of(expiredBooking); }
            @Override
            public Optional<Booking> findByIdForUpdate(Long id) { return Optional.of(expiredBooking); }
            @Override
            public Optional<Booking> findByReference(String bookingReference) { return Optional.of(expiredBooking); }
            @Override
            public Optional<Booking> findByReferenceForUpdate(String bookingReference) { return Optional.of(expiredBooking); }
            @Override
            public Set<String> findActiveBookedSeatNumbers(Long tripId, LocalDateTime now) { return Set.of(); }
            @Override
            public PageResponse<Booking> findByUserId(Long userId, int page, int size) { return null; }
            @Override
            public PageResponse<Booking> findAllWithFilters(BookingStatus status, Long tripId, Long userId, int page, int size) { return null; }
            @Override
            public List<Booking> findExpiredHeldBookings(LocalDateTime now, int limit) { return List.of(); }
            @Override
            public Booking save(Booking booking) { return booking; }
        };

        TransactionTemplate sharedTxTemplate = new TransactionTemplate(strictTxManager);
        BookingCommandServiceImpl bookingCommandService = new BookingCommandServiceImpl(
                bookingRepository,
                mock(SeatLockRepository.class),
                mock(CatalogTripPort.class),
                mock(IdentityUserPort.class),
                mock(PaymentRefundPort.class),
                sharedTxTemplate,
                mock(ApplicationEventPublisher.class)
        );

        PaymentBookingConfirmPortAdapter confirmPortAdapter =
                new PaymentBookingConfirmPortAdapter(bookingCommandService, bookingRepository);

        Payment pendingPayment = Payment.builder()
                .id(888L)
                .paymentReference("PAY_EXPIRED_888")
                .bookingId(777L)
                .amount(BigDecimal.valueOf(600000))
                .status(PaymentStatus.PENDING)
                .build();

        PaymentRepository paymentRepository = new PaymentRepository() {
            @Override
            public Optional<Payment> findById(Long id) { return Optional.of(pendingPayment); }
            @Override
            public Optional<Payment> findByPaymentReference(String paymentReference) { return Optional.of(pendingPayment); }
            @Override
            public Optional<Payment> findByPaymentReferenceForUpdate(String paymentReference) { return Optional.of(pendingPayment); }
            @Override
            public Optional<Payment> findFirstByBookingIdAndStatusOrderByIdDesc(Long bookingId, PaymentStatus status) { return Optional.empty(); }
            @Override
            public Optional<Payment> findFirstByBookingIdAndStatus(Long bookingId, PaymentStatus status) { return Optional.empty(); }
            @Override
            public com.ticket.payment.domain.model.PaymentRevenueReport getRevenueReport(LocalDateTime from, LocalDateTime to) { return null; }
            @Override
            public PageResponse<Payment> findPaymentsWithFilters(PaymentStatus status, PaymentMethod paymentMethod, String paymentReference, Boolean reconciliationRequired, int page, int size) { return null; }
            @Override
            public Payment save(Payment payment) { return payment; }
        };

        VnPayProperties props = new VnPayProperties();
        props.setHashSecret("secret_key_for_tx_test");

        PaymentServiceImpl paymentService = new PaymentServiceImpl(
                paymentRepository,
                mock(PaymentBookingPort.class),
                confirmPortAdapter,
                mock(PaymentIdentityPort.class),
                props,
                mock(ApplicationEventPublisher.class)
        );

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "60000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "TXN_999");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "PAY_EXPIRED_888");
        String signData = "vnp_Amount=60000000&vnp_ResponseCode=00&vnp_TransactionNo=TXN_999&vnp_TransactionStatus=00&vnp_TxnRef=PAY_EXPIRED_888";
        params.put("vnp_SecureHash", HmacSha512Util.hmacSHA512("secret_key_for_tx_test", signData));

        VnPayCallbackDto callbackDto = new VnPayCallbackDto();
        callbackDto.setVnp_TxnRef("PAY_EXPIRED_888");
        callbackDto.setVnp_Amount("60000000");
        callbackDto.setVnp_ResponseCode("00");
        callbackDto.setVnp_TransactionStatus("00");
        callbackDto.setVnp_TransactionNo("TXN_999");
        callbackDto.setAllParams(params);

        VnPayIpnResponse response = sharedTxTemplate.execute(status -> paymentService.execute(callbackDto));

        assertThat(transactionMarkedRollbackOnly.get()).isFalse();
        assertThat(response).isNotNull();
        assertThat(response.getRspCode()).isEqualTo("00");
        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(pendingPayment.isReconciliationRequired()).isTrue();
        assertThat(pendingPayment.getRefundReason()).isEqualTo("PAID_AFTER_BOOKING_EXPIRED");
    }
}
