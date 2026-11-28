package com.ticket.concurrency;

import com.ticket.TicketApplication;
import com.ticket.partner.application.usecase.PartnerAdminService;
import com.ticket.partner.domain.model.OperatorStatus;
import com.ticket.partner.domain.model.WithdrawalStatus;
import com.ticket.partner.infrastructure.persistence.entity.BusOperatorJpaEntity;
import com.ticket.partner.infrastructure.persistence.entity.OperatorWalletJpaEntity;
import com.ticket.partner.infrastructure.persistence.entity.WalletTransactionJpaEntity;
import com.ticket.partner.infrastructure.persistence.entity.WithdrawalRequestJpaEntity;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataBusOperatorRepository;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataOperatorWalletRepository;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataWalletTransactionRepository;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataWithdrawalRequestRepository;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FQA-01: Real MySQL 8.x InnoDB & Spring Transaction Proxy Concurrency Verification.
 * Tests real database transaction isolation, optimistic locking on OperatorWallet,
 * and strict terminal state transition invariants without test-side synchronization locks.
 */
@SpringBootTest(classes = TicketApplication.class)
@ActiveProfiles("test")
@DisplayName("FQA-01: Real MySQL Financial Withdrawal Concurrency & Invariants Integration Suite")
class RealMySqlWithdrawalConcurrencyIT {

    @Autowired
    private PartnerAdminService adminService;

    @Autowired
    private SpringDataBusOperatorRepository operatorRepository;

    @Autowired
    private SpringDataOperatorWalletRepository walletRepository;

    @Autowired
    private SpringDataWithdrawalRequestRepository withdrawalRepository;

    @Autowired
    private SpringDataWalletTransactionRepository transactionRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testOperatorId;
    private Long testWalletId;

    @BeforeAll
    static void checkMySqlReachable() {
        boolean reachable = false;
        String host = System.getenv().getOrDefault("MYSQL_HOST", "127.0.0.1");
        int port = Integer.parseInt(System.getenv().getOrDefault("MYSQL_PORT", "3307"));
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 1500);
            reachable = true;
        } catch (Exception ignored) {
        }
        Assumptions.assumeTrue(reachable, "MySQL server must be reachable on " + host + ":" + port + " for RealMySqlWithdrawalConcurrencyIT");
    }

    @BeforeEach
    void setUpRealDatabaseFixtures() {
        // Create an isolated BusOperator and OperatorWallet in MySQL
        String uid = UUID.randomUUID().toString().substring(0, 8);
        BusOperatorJpaEntity op = BusOperatorJpaEntity.builder()
                .userId(Math.abs((long) uid.hashCode()))
                .businessName("CONG TY TNHH VAN TAI " + uid.toUpperCase())
                .brandName("Nha Xe " + uid.toUpperCase())
                .taxCode("0" + (100000000 + Math.abs(uid.hashCode() % 900000000)))
                .contactPhone("098" + (1000000 + Math.abs(uid.hashCode() % 9000000)))
                .contactEmail("operator_" + uid + "@velobus.vn")
                .commissionRate(BigDecimal.valueOf(10.0))
                .status(OperatorStatus.ACTIVE)
                .build();
        op = operatorRepository.save(op);
        testOperatorId = op.getId();

        OperatorWalletJpaEntity wallet = OperatorWalletJpaEntity.builder()
                .operatorId(testOperatorId)
                .availableBalance(BigDecimal.valueOf(50000000))
                .pendingSettlementBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.valueOf(20000000))
                .debtBalance(BigDecimal.ZERO)
                .currency("VND")
                .version(0L)
                .build();
        wallet = walletRepository.save(wallet);
        testWalletId = wallet.getId();
    }

    private Long createPendingWithdrawal(BigDecimal amount) {
        WithdrawalRequestJpaEntity wdr = WithdrawalRequestJpaEntity.builder()
                .operatorId(testOperatorId)
                .amount(amount)
                .bankName("Vietcombank")
                .accountNumber("0123456789")
                .accountHolderName("NGUYEN VAN CONCURRENCY")
                .status(WithdrawalStatus.PENDING)
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
        return withdrawalRepository.save(wdr).getId();
    }

    private boolean isExpectedConcurrencyConflict(Throwable t) {
        if (t instanceof BusinessException be) {
            String code = be.getCode();
            String msg = be.getMessage() != null ? be.getMessage().toUpperCase() : "";
            return "INVALID_STATUS".equalsIgnoreCase(code) || msg.contains("PENDING") || msg.contains("STATUS") || msg.contains("NOT FOUND");
        }
        if (t instanceof org.springframework.dao.ConcurrencyFailureException) {
            return true;
        }
        if (t instanceof org.springframework.transaction.TransactionException) {
            return true;
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            return isExpectedConcurrencyConflict(cause);
        }
        return false;
    }

    @Test
    @DisplayName("FQA-01.1: Concurrent approve/approve on same withdrawal ensures exactly 1 success and 1 valid terminal state")
    void testConcurrentApproveApprove() throws Exception {
        BigDecimal wdrAmount = BigDecimal.valueOf(5000000);
        Long wdrId = createPendingWithdrawal(wdrAmount);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final long adminId = 100L + i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    adminService.approveWithdrawal(wdrId, adminId, "Concurrent Approve Real IT");
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    if (isExpectedConcurrencyConflict(t)) {
                        conflictCount.incrementAndGet();
                    } else {
                        throw new RuntimeException("Unexpected exception in approve/approve concurrency test", t);
                    }
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        // Exactly one approval succeeds
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // Verify committed database state
        WithdrawalRequestJpaEntity committedWdr = withdrawalRepository.findById(wdrId).orElseThrow();
        assertThat(committedWdr.getStatus()).isEqualTo(WithdrawalStatus.APPROVED);

        OperatorWalletJpaEntity committedWallet = walletRepository.findById(testWalletId).orElseThrow();
        // Locked balance must be debited exactly once: 20,000,000 - 5,000,000 = 15,000,000
        assertThat(committedWallet.getLockedBalance()).isEqualByComparingTo(BigDecimal.valueOf(15000000));
        assertThat(committedWallet.getAvailableBalance()).isEqualByComparingTo(BigDecimal.valueOf(50000000));

        // Ledger must contain exactly 1 transaction for this withdrawal
        List<WalletTransactionJpaEntity> txs = transactionRepository.findAll().stream()
                .filter(t -> t.getOperatorId().equals(testOperatorId))
                .toList();
        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).getNetAmount()).isEqualByComparingTo(wdrAmount.negate());
    }

    @Test
    @DisplayName("FQA-01.2: Concurrent approve/reject on same withdrawal yields exactly 1 terminal state (no double ledger effect)")
    void testConcurrentApproveReject() throws Exception {
        BigDecimal wdrAmount = BigDecimal.valueOf(3000000);
        Long wdrId = createPendingWithdrawal(wdrAmount);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        // Thread 0: Approve
        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                adminService.approveWithdrawal(wdrId, 901L, "Approve Thread");
                successCount.incrementAndGet();
            } catch (Throwable t) {
                if (isExpectedConcurrencyConflict(t)) {
                    conflictCount.incrementAndGet();
                } else {
                    throw new RuntimeException("Unexpected exception in approve thread", t);
                }
            }
        });

        // Thread 1: Reject
        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                adminService.rejectWithdrawal(wdrId, 902L, "Reject Thread");
                successCount.incrementAndGet();
            } catch (Throwable t) {
                if (isExpectedConcurrencyConflict(t)) {
                    conflictCount.incrementAndGet();
                } else {
                    throw new RuntimeException("Unexpected exception in reject thread", t);
                }
            }
        });

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        WithdrawalRequestJpaEntity committedWdr = withdrawalRepository.findById(wdrId).orElseThrow();
        assertThat(committedWdr.getStatus()).isIn(WithdrawalStatus.APPROVED, WithdrawalStatus.REJECTED);

        OperatorWalletJpaEntity committedWallet = walletRepository.findById(testWalletId).orElseThrow();
        if (committedWdr.getStatus() == WithdrawalStatus.APPROVED) {
            // Locked balance debited, available unchanged
            assertThat(committedWallet.getLockedBalance()).isEqualByComparingTo(BigDecimal.valueOf(17000000));
            assertThat(committedWallet.getAvailableBalance()).isEqualByComparingTo(BigDecimal.valueOf(50000000));
        } else {
            // Locked balance released back to available balance
            assertThat(committedWallet.getLockedBalance()).isEqualByComparingTo(BigDecimal.valueOf(17000000));
            assertThat(committedWallet.getAvailableBalance()).isEqualByComparingTo(BigDecimal.valueOf(53000000));
        }
    }

    @Test
    @DisplayName("FQA-01.3: Concurrent reject/reject on same withdrawal yields exactly 1 refund transaction")
    void testConcurrentRejectReject() throws Exception {
        BigDecimal wdrAmount = BigDecimal.valueOf(2000000);
        Long wdrId = createPendingWithdrawal(wdrAmount);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final long adminId = 800L + i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    adminService.rejectWithdrawal(wdrId, adminId, "Reject duplicate check");
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    if (isExpectedConcurrencyConflict(t)) {
                        conflictCount.incrementAndGet();
                    } else {
                        throw new RuntimeException("Unexpected exception in reject duplicate check", t);
                    }
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        WithdrawalRequestJpaEntity committedWdr = withdrawalRepository.findById(wdrId).orElseThrow();
        assertThat(committedWdr.getStatus()).isEqualTo(WithdrawalStatus.REJECTED);

        OperatorWalletJpaEntity committedWallet = walletRepository.findById(testWalletId).orElseThrow();
        assertThat(committedWallet.getLockedBalance()).isEqualByComparingTo(BigDecimal.valueOf(18000000));
        assertThat(committedWallet.getAvailableBalance()).isEqualByComparingTo(BigDecimal.valueOf(52000000));
    }

    @Test
    @DisplayName("FQA-01.4: Concurrent processing of distinct withdrawals against same wallet protects version consistency")
    void testConcurrentDistinctWithdrawalsAgainstSameWallet() throws Exception {
        BigDecimal amountA = BigDecimal.valueOf(1000000);
        BigDecimal amountB = BigDecimal.valueOf(2000000);
        Long wdrA = createPendingWithdrawal(amountA);
        Long wdrB = createPendingWithdrawal(amountB);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger optimisticLockCount = new AtomicInteger(0);

        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                adminService.approveWithdrawal(wdrA, 701L, "Approve A");
                completedCount.incrementAndGet();
            } catch (Throwable t) {
                if (isExpectedConcurrencyConflict(t)) {
                    optimisticLockCount.incrementAndGet();
                } else {
                    throw new RuntimeException("Unexpected exception during distinct withdrawal A", t);
                }
            }
        });

        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                adminService.approveWithdrawal(wdrB, 702L, "Approve B");
                completedCount.incrementAndGet();
            } catch (Throwable t) {
                if (isExpectedConcurrencyConflict(t)) {
                    optimisticLockCount.incrementAndGet();
                } else {
                    throw new RuntimeException("Unexpected exception during distinct withdrawal B", t);
                }
            }
        });

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        // Either both serialize or one fails with OptimisticLockingFailureException (preventing lost update)
        assertThat(completedCount.get() + optimisticLockCount.get()).isEqualTo(2);

        OperatorWalletJpaEntity finalWallet = walletRepository.findById(testWalletId).orElseThrow();
        if (completedCount.get() == 2) {
            assertThat(finalWallet.getLockedBalance()).isEqualByComparingTo(BigDecimal.valueOf(17000000));
        } else {
            // Exactly one updated
            assertThat(finalWallet.getLockedBalance()).isIn(BigDecimal.valueOf(19000000), BigDecimal.valueOf(18000000));
        }
    }

    @Test
    @DisplayName("FQA-01.5: Transaction rollback on failure ensures zero partial mutations in wallet, withdrawal, or ledger")
    void testTransactionRollbackOnFailure() {
        BigDecimal wdrAmount = BigDecimal.valueOf(4000000);
        Long wdrId = createPendingWithdrawal(wdrAmount);

        OperatorWalletJpaEntity initialWallet = walletRepository.findById(testWalletId).orElseThrow();
        BigDecimal initialLocked = initialWallet.getLockedBalance();
        BigDecimal initialAvailable = initialWallet.getAvailableBalance();

        long initialTxCount = transactionRepository.findAll().stream()
                .filter(t -> t.getOperatorId().equals(testOperatorId))
                .count();

        // Attempt an atomic operation that encounters an unhandled runtime failure after partial steps
        try {
            transactionTemplate.execute(status -> {
                adminService.approveWithdrawal(wdrId, 999L, "Rollback Test Approval");
                // Deliberately trigger failure before commit
                throw new IllegalStateException("Simulated transient infrastructure crash before transaction commit");
            });
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage()).contains("Simulated transient infrastructure crash");
        }

        // Verify that MySQL rolled back all changes cleanly
        WithdrawalRequestJpaEntity rolledBackWdr = withdrawalRepository.findById(wdrId).orElseThrow();
        assertThat(rolledBackWdr.getStatus()).isEqualTo(WithdrawalStatus.PENDING);

        OperatorWalletJpaEntity rolledBackWallet = walletRepository.findById(testWalletId).orElseThrow();
        assertThat(rolledBackWallet.getLockedBalance()).isEqualByComparingTo(initialLocked);
        assertThat(rolledBackWallet.getAvailableBalance()).isEqualByComparingTo(initialAvailable);

        long finalTxCount = transactionRepository.findAll().stream()
                .filter(t -> t.getOperatorId().equals(testOperatorId))
                .count();
        assertThat(finalTxCount).isEqualTo(initialTxCount);
    }
}
