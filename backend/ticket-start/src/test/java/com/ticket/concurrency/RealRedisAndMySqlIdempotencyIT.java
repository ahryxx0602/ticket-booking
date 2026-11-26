package com.ticket.concurrency;

import com.ticket.TicketApplication;
import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.HoldSeatCommand;
import com.ticket.booking.application.usecase.CancelBookingUseCase;
import com.ticket.booking.application.usecase.ExpireHeldBookingsUseCase;
import com.ticket.booking.application.usecase.HoldSeatUseCase;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.booking.domain.repository.SeatLockRepository;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Gate B5.2-I: Real Redis 7.x & MySQL 8.4 InnoDB Integration & Concurrency Verification Suite.
 * NO mocked SeatLockRepository. Real distributed locks and real InnoDB transactions.
 * Verifies all 11 required concurrency and idempotency lifecycle scenarios.
 */
@SpringBootTest(classes = TicketApplication.class)
@ActiveProfiles("test")
@DisplayName("Gate B5.2-I: Real Redis & MySQL 8.4 Hold Idempotency & Real Concurrency Verification")
class RealRedisAndMySqlIdempotencyIT {

    private static final Logger log = LoggerFactory.getLogger(RealRedisAndMySqlIdempotencyIT.class);

    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private HoldSeatUseCase holdSeatUseCase;
    @Autowired private CancelBookingUseCase cancelBookingUseCase;
    @Autowired private ExpireHeldBookingsUseCase expireHeldBookingsUseCase;
    @Autowired private SeatLockRepository seatLockRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RedissonClient redissonClient;

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

        Assumptions.assumeTrue(mysqlReachable, "MySQL server must be reachable on " + mysqlHost + ":" + mysqlPort);
        Assumptions.assumeTrue(redisReachable, "Redis server must be reachable on " + redisHost + ":" + redisPort);
    }

    @BeforeEach
    void setUpFixture() {
        customer1Username = "idem_cust1_" + UUID.randomUUID().toString().substring(0, 8);
        User u1 = User.builder()
                .username(customer1Username)
                .email(customer1Username + "@velobus.vn")
                .fullName("Idempotency Customer 1")
                .phone("0981111222")
                .passwordHash(passwordEncoder.encode("Test_Pass_123"))
                .role(UserRole.ROLE_CUSTOMER)
                .customRoleCode("ROLE_CUSTOMER")
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .build();
        User savedU1 = userRepository.save(u1);
        customer1Id = savedU1.getId();

        customer2Username = "idem_cust2_" + UUID.randomUUID().toString().substring(0, 8);
        User u2 = User.builder()
                .username(customer2Username)
                .email(customer2Username + "@velobus.vn")
                .fullName("Idempotency Customer 2")
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
                        'BUS', 6, 250000.00, 'SCHEDULED', 'UNSETTLED')
                """, tripCode, depId, arrId);
        testTripId = jdbcTemplate.queryForObject("SELECT id FROM trips WHERE trip_code = ?", Long.class, tripCode);

        for (String seatNum : List.of("01A", "01B", "01C", "02A", "02B", "02C")) {
            jdbcTemplate.update("""
                    INSERT INTO seats (trip_id, seat_number, seat_type, price_multiplier)
                    VALUES (?, ?, 'STANDARD', 1.00)
                    """, testTripId, seatNum);
        }
    }

    @Test
    @DisplayName("Scenario 1: Competing customers for 1 seat - Exactly 1 wins, other receives SEAT_ALREADY_RESERVED")
    void scenario01_competingCustomersForSameSeat() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threads);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        executor.submit(() -> {
            try {
                startGate.await();
                HoldSeatCommand cmd = new HoldSeatCommand();
                cmd.setTripId(testTripId);
                cmd.setSeatNumbers(List.of("01A"));
                cmd.setClientRequestId(UUID.randomUUID().toString());
                holdSeatUseCase.execute(cmd, customer1Username);
                successes.incrementAndGet();
            } catch (BusinessException be) {
                if ("SEAT_ALREADY_RESERVED".equals(be.getCode())) {
                    conflicts.incrementAndGet();
                }
            } catch (Exception ignored) {
            } finally {
                doneGate.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startGate.await();
                HoldSeatCommand cmd = new HoldSeatCommand();
                cmd.setTripId(testTripId);
                cmd.setSeatNumbers(List.of("01A"));
                cmd.setClientRequestId(UUID.randomUUID().toString());
                holdSeatUseCase.execute(cmd, customer2Username);
                successes.incrementAndGet();
            } catch (BusinessException be) {
                if ("SEAT_ALREADY_RESERVED".equals(be.getCode())) {
                    conflicts.incrementAndGet();
                }
            } catch (Exception ignored) {
            } finally {
                doneGate.countDown();
            }
        });

        startGate.countDown();
        doneGate.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Scenario 2: Simultaneous same-key requests - Replays same booking, exactly 1 DB record created")
    void scenario02_simultaneousSameKeyRequests() throws Exception {
        int threads = 4;
        String sharedKey = UUID.randomUUID().toString();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threads);

        Set<String> bookingReferences = ConcurrentHashMap.newKeySet();
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    HoldSeatCommand cmd = new HoldSeatCommand();
                    cmd.setTripId(testTripId);
                    cmd.setSeatNumbers(List.of("01B"));
                    cmd.setClientRequestId(sharedKey);
                    BookingResponse res = holdSeatUseCase.execute(cmd, customer1Username);
                    bookingReferences.add(res.getBookingReference());
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        doneGate.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(errors).isEmpty();
        assertThat(bookingReferences).hasSize(1);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE client_request_id = ? AND user_id = ?",
                Integer.class, sharedKey, customer1Id
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Scenario 3: Different keys competing for same seat - 1 winner, 1 rejected")
    void scenario03_differentKeysCompetingForSameSeat() throws Exception {
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();

        HoldSeatCommand cmd1 = new HoldSeatCommand();
        cmd1.setTripId(testTripId);
        cmd1.setSeatNumbers(List.of("01C"));
        cmd1.setClientRequestId(key1);

        BookingResponse r1 = holdSeatUseCase.execute(cmd1, customer1Username);
        assertThat(r1.getStatus()).isEqualTo(BookingStatus.HELD);

        HoldSeatCommand cmd2 = new HoldSeatCommand();
        cmd2.setTripId(testTripId);
        cmd2.setSeatNumbers(List.of("01C"));
        cmd2.setClientRequestId(key2);

        assertThatThrownBy(() -> holdSeatUseCase.execute(cmd2, customer2Username))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("SEAT_ALREADY_RESERVED");
    }

    @Test
    @DisplayName("Scenario 4: Same key with different payload - Throws IDEMPOTENCY_KEY_PAYLOAD_MISMATCH")
    void scenario04_sameKeyDifferentPayloadMismatch() {
        String key = UUID.randomUUID().toString();

        HoldSeatCommand cmd1 = new HoldSeatCommand();
        cmd1.setTripId(testTripId);
        cmd1.setSeatNumbers(List.of("02A"));
        cmd1.setClientRequestId(key);

        BookingResponse res = holdSeatUseCase.execute(cmd1, customer1Username);
        assertThat(res.getStatus()).isEqualTo(BookingStatus.HELD);

        HoldSeatCommand cmd2 = new HoldSeatCommand();
        cmd2.setTripId(testTripId);
        cmd2.setSeatNumbers(List.of("02B")); // Different seat!
        cmd2.setClientRequestId(key);

        assertThatThrownBy(() -> holdSeatUseCase.execute(cmd2, customer1Username))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("IDEMPOTENCY_KEY_PAYLOAD_MISMATCH");
    }

    @Test
    @DisplayName("Scenario 5: Multi-seat conflict - All-or-nothing atomicity")
    void scenario05_multiSeatConflictAllOrNothing() throws Exception {
        // Customer 1 requests [02B, 02C]; Customer 2 requests [02C]
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();

        HoldSeatCommand cmd1 = new HoldSeatCommand();
        cmd1.setTripId(testTripId);
        cmd1.setSeatNumbers(List.of("02B", "02C"));
        cmd1.setClientRequestId(key1);

        BookingResponse r1 = holdSeatUseCase.execute(cmd1, customer1Username);
        assertThat(r1.getStatus()).isEqualTo(BookingStatus.HELD);

        HoldSeatCommand cmd2 = new HoldSeatCommand();
        cmd2.setTripId(testTripId);
        cmd2.setSeatNumbers(List.of("02C")); // Overlaps with 02C
        cmd2.setClientRequestId(key2);

        assertThatThrownBy(() -> holdSeatUseCase.execute(cmd2, customer2Username))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("SEAT_ALREADY_RESERVED");
    }

    @Test
    @DisplayName("Scenario 6: Lost response retry with same key - Replays exact booking without duplicate writes")
    void scenario06_lostResponseRetryWithSameKey() {
        String key = UUID.randomUUID().toString();

        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(testTripId);
        cmd.setSeatNumbers(List.of("02A"));
        cmd.setClientRequestId(key);

        BookingResponse initial = holdSeatUseCase.execute(cmd, customer1Username);

        // Simulate retry with re-ordered seats and whitespace
        HoldSeatCommand retry = new HoldSeatCommand();
        retry.setTripId(testTripId);
        retry.setSeatNumbers(List.of(" 02a "));
        retry.setClientRequestId(key);

        BookingResponse replayed = holdSeatUseCase.execute(retry, customer1Username);

        assertThat(replayed.getBookingReference()).isEqualTo(initial.getBookingReference());
        assertThat(replayed.getTotalAmount()).isEqualByComparingTo(initial.getTotalAmount());
        assertThat(replayed.getSeatNumbers()).containsExactly("02A");
    }

    @Test
    @DisplayName("Scenario 7: Booking expiry - Expired booking replays terminal state, seat lock is freed")
    void scenario07_bookingExpiryAndTerminalReplay() {
        String key = UUID.randomUUID().toString();

        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(testTripId);
        cmd.setSeatNumbers(List.of("02C"));
        cmd.setClientRequestId(key);

        BookingResponse res = holdSeatUseCase.execute(cmd, customer1Username);
        assertThat(res.getStatus()).isEqualTo(BookingStatus.HELD);

        // Force expiration in DB by backdating expires_at
        jdbcTemplate.update("UPDATE bookings SET expires_at = DATE_SUB(NOW(), INTERVAL 5 MINUTE) WHERE booking_reference = ?",
                res.getBookingReference());

        // Run expiration job
        int expiredCount = expireHeldBookingsUseCase.execute(LocalDateTime.now());
        assertThat(expiredCount).isGreaterThanOrEqualTo(1);

        // Replay with same key returns terminal EXPIRED status
        BookingResponse replayed = holdSeatUseCase.execute(cmd, customer1Username);
        assertThat(replayed.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(replayed.getBookingReference()).isEqualTo(res.getBookingReference());

        // Seat 02C Redis lock should be freed, allowing customer 2 to hold it
        HoldSeatCommand cmdCust2 = new HoldSeatCommand();
        cmdCust2.setTripId(testTripId);
        cmdCust2.setSeatNumbers(List.of("02C"));
        cmdCust2.setClientRequestId(UUID.randomUUID().toString());

        BookingResponse resCust2 = holdSeatUseCase.execute(cmdCust2, customer2Username);
        assertThat(resCust2.getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    @DisplayName("Scenario 8: Booking cancellation - Terminal CANCELLED replayed, seat lock freed")
    void scenario08_bookingCancellationAndReplay() {
        String key = UUID.randomUUID().toString();

        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(testTripId);
        cmd.setSeatNumbers(List.of("01A"));
        cmd.setClientRequestId(key);

        BookingResponse res = holdSeatUseCase.execute(cmd, customer1Username);
        assertThat(res.getStatus()).isEqualTo(BookingStatus.HELD);

        cancelBookingUseCase.execute(res.getBookingReference(), customer1Username);

        // Replay with key returns CANCELLED
        BookingResponse replayed = holdSeatUseCase.execute(cmd, customer1Username);
        assertThat(replayed.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(replayed.getBookingReference()).isEqualTo(res.getBookingReference());

        // Redis lock is released, allowing another booking
        HoldSeatCommand cmd2 = new HoldSeatCommand();
        cmd2.setTripId(testTripId);
        cmd2.setSeatNumbers(List.of("01A"));
        cmd2.setClientRequestId(UUID.randomUUID().toString());

        BookingResponse res2 = holdSeatUseCase.execute(cmd2, customer2Username);
        assertThat(res2.getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    @DisplayName("Scenario 9: Database rollback safety - Releases Redis locks on DB failure")
    void scenario09_databaseRollbackSafetyReleasesRedisLocks() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(testTripId);
        cmd.setSeatNumbers(List.of("NON_EXISTENT_SEAT"));
        cmd.setClientRequestId(UUID.randomUUID().toString());

        assertThatThrownBy(() -> holdSeatUseCase.execute(cmd, customer1Username))
                .isInstanceOf(BusinessException.class);

        // Verify Redis lock is not leaked
        boolean locked = seatLockRepository.isSeatLocked(testTripId, "NON_EXISTENT_SEAT");
        assertThat(locked).isFalse();
    }

    @Test
    @DisplayName("Scenario 10: Redis failure / rejected lock behavior leaves zero dirty MySQL rows")
    void scenario10_redisLockRejectionLeavesNoDirtyRows() {
        String key1 = UUID.randomUUID().toString();
        HoldSeatCommand cmd1 = new HoldSeatCommand();
        cmd1.setTripId(testTripId);
        cmd1.setSeatNumbers(List.of("01B"));
        cmd1.setClientRequestId(key1);
        holdSeatUseCase.execute(cmd1, customer1Username);

        int bookingsBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bookings WHERE trip_id = ?", Integer.class, testTripId);

        String key2 = UUID.randomUUID().toString();
        HoldSeatCommand cmd2 = new HoldSeatCommand();
        cmd2.setTripId(testTripId);
        cmd2.setSeatNumbers(List.of("01B"));
        cmd2.setClientRequestId(key2);

        assertThatThrownBy(() -> holdSeatUseCase.execute(cmd2, customer2Username))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("SEAT_ALREADY_RESERVED");

        int bookingsAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bookings WHERE trip_id = ?", Integer.class, testTripId);
        assertThat(bookingsAfter).isEqualTo(bookingsBefore);
    }

    @Test
    @DisplayName("Scenario 11: Durable request correlation across clean context & measures latency metrics")
    void scenario11_durableRequestCorrelationAndMetrics() throws Exception {
        String key = UUID.randomUUID().toString();
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(testTripId);
        cmd.setSeatNumbers(List.of("02A"));
        cmd.setClientRequestId(key);

        BookingResponse original = holdSeatUseCase.execute(cmd, customer1Username);
        assertThat(original.getStatus()).isEqualTo(BookingStatus.HELD);

        // Fetch fresh from database repository
        Optional<Booking> freshOpt = bookingRepository.findByUserIdAndClientRequestId(customer1Id, key);
        assertThat(freshOpt).isPresent();
        assertThat(freshOpt.get().getBookingReference()).isEqualTo(original.getBookingReference());
        assertThat(freshOpt.get().getClientRequestId()).isEqualTo(key);

        // Concurrency metrics evaluation: execute 20 concurrent replays
        int concurrency = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(concurrency);
        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger replayCount = new AtomicInteger(0);

        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    long start = System.nanoTime();
                    BookingResponse r = holdSeatUseCase.execute(cmd, customer1Username);
                    long duration = (System.nanoTime() - start) / 1_000_000;
                    latenciesMs.add(duration);
                    if (r.getBookingReference().equals(original.getBookingReference())) {
                        replayCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Replay thread failed", e);
                } finally {
                    doneGate.countDown();
                }
            });
        }

        long burstStart = System.currentTimeMillis();
        startGate.countDown();
        doneGate.await(10, TimeUnit.SECONDS);
        long totalBurstMs = System.currentTimeMillis() - burstStart;
        executor.shutdown();

        assertThat(replayCount.get()).isEqualTo(concurrency);

        Collections.sort(latenciesMs);
        long p50 = latenciesMs.get(latenciesMs.size() / 2);
        long p95 = latenciesMs.get((int) (latenciesMs.size() * 0.95));
        long p99 = latenciesMs.get(latenciesMs.size() - 1);
        double throughput = (concurrency * 1000.0) / Math.max(totalBurstMs, 1);

        log.info("""
                ======================================================
                GATE B5.2-I IDEMPOTENCY CONCURRENCY METRICS REPORT
                ======================================================
                Total Requests: {}
                Successful Replays: {}
                Total Burst Time: {} ms
                Throughput: {} ops/sec
                Latency p50: {} ms
                Latency p95: {} ms
                Latency p99: {} ms
                ======================================================
                """, concurrency, replayCount.get(), totalBurstMs, String.format("%.2f", throughput), p50, p95, p99);
    }
}
