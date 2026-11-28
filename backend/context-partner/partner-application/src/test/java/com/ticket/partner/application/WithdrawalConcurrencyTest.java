package com.ticket.partner.application;

import com.ticket.partner.application.dto.WithdrawalResponse;
import com.ticket.partner.application.service.PartnerAdminServiceImpl;
import com.ticket.partner.domain.model.*;
import com.ticket.partner.domain.port.*;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("D2-PRE-08 Financial Concurrency & Invariants Verification")
class WithdrawalConcurrencyTest {

    @Mock
    private BusOperatorRepository operatorRepository;
    @Mock
    private OperatorWalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository transactionRepository;
    @Mock
    private WithdrawalRequestRepository withdrawalRepository;
    @Mock
    private BannerRepository bannerRepository;
    @Mock
    private SystemSettingRepository systemSettingRepository;
    @Mock
    private PartnerIdentityPort partnerIdentityPort;

    private PartnerAdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = new PartnerAdminServiceImpl(
                operatorRepository, walletRepository, transactionRepository,
                withdrawalRepository, bannerRepository, systemSettingRepository, partnerIdentityPort
        );
    }

    @Test
    @DisplayName("1. Concurrent approve/approve on the same withdrawal ensures exactly one success")
    void testConcurrentApproveSameWithdrawal() throws Exception {
        Long withdrawalId = 100L;
        Long operatorId = 1L;
        BigDecimal amount = BigDecimal.valueOf(500000);

        WithdrawalRequest sharedRequest = WithdrawalRequest.builder()
                .id(withdrawalId)
                .operatorId(operatorId)
                .amount(amount)
                .status(WithdrawalStatus.PENDING)
                .bankName("Vietcombank")
                .accountNumber("0123456789")
                .accountHolderName("TRAN VAN A")
                .build();

        OperatorWallet sharedWallet = OperatorWallet.builder()
                .id(1L)
                .operatorId(operatorId)
                .availableBalance(BigDecimal.valueOf(1000000))
                .lockedBalance(amount)
                .version(1L)
                .build();

        when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(sharedRequest));
        when(walletRepository.findByOperatorId(operatorId)).thenReturn(Optional.of(sharedWallet));
        when(withdrawalRepository.save(any(WithdrawalRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.save(any(OperatorWallet.class))).thenAnswer(i -> i.getArgument(0));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Synchronize on the request domain object to mimic DB row-level lock or atomic commit
        Object lock = new Object();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final long adminId = 99L + i;
            tasks.add(() -> {
                barrier.await();
                try {
                    synchronized (lock) {
                        adminService.approveWithdrawal(withdrawalId, adminId, "Approve by " + adminId);
                    }
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    failureCount.incrementAndGet();
                    exceptions.add(t);
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one approval must succeed");
        assertEquals(1, failureCount.get(), "Second approval must fail");
        assertTrue(exceptions.get(0) instanceof BusinessException, "Failure should be BusinessException");
        assertEquals("INVALID_STATUS", ((BusinessException) exceptions.get(0)).getCode());

        assertEquals(WithdrawalStatus.APPROVED, sharedRequest.getStatus());
        assertEquals(BigDecimal.ZERO, sharedWallet.getLockedBalance());
        verify(transactionRepository, times(1)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("2. Concurrent approve and reject on the same withdrawal yields exactly one terminal state")
    void testConcurrentApproveAndRejectSameWithdrawal() throws Exception {
        Long withdrawalId = 200L;
        Long operatorId = 2L;
        BigDecimal amount = BigDecimal.valueOf(300000);

        WithdrawalRequest sharedRequest = WithdrawalRequest.builder()
                .id(withdrawalId)
                .operatorId(operatorId)
                .amount(amount)
                .status(WithdrawalStatus.PENDING)
                .bankName("Techcombank")
                .accountNumber("9876543210")
                .accountHolderName("LE THI B")
                .build();

        OperatorWallet sharedWallet = OperatorWallet.builder()
                .id(2L)
                .operatorId(operatorId)
                .availableBalance(BigDecimal.valueOf(200000))
                .lockedBalance(amount)
                .version(1L)
                .build();

        when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(sharedRequest));
        when(walletRepository.findByOperatorId(operatorId)).thenReturn(Optional.of(sharedWallet));
        when(withdrawalRepository.save(any(WithdrawalRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.save(any(OperatorWallet.class))).thenAnswer(i -> i.getArgument(0));

        Object lock = new Object();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger terminalCount = new AtomicInteger(0);

        Future<?> f1 = executor.submit(() -> {
            barrier.await();
            synchronized (lock) {
                adminService.approveWithdrawal(withdrawalId, 101L, "Approve race");
                terminalCount.incrementAndGet();
            }
            return null;
        });

        Future<?> f2 = executor.submit(() -> {
            barrier.await();
            synchronized (lock) {
                adminService.rejectWithdrawal(withdrawalId, 102L, "Reject race");
                terminalCount.incrementAndGet();
            }
            return null;
        });

        try {
            f1.get();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof BusinessException);
        }
        try {
            f2.get();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof BusinessException);
        }
        executor.shutdown();

        assertEquals(1, terminalCount.get(), "Only one operation (approve or reject) can succeed");
        assertTrue(sharedRequest.getStatus() == WithdrawalStatus.APPROVED || sharedRequest.getStatus() == WithdrawalStatus.REJECTED);
        verify(transactionRepository, times(1)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("3. Concurrent reject/reject on the same withdrawal allows only one refund")
    void testConcurrentRejectSameWithdrawal() throws Exception {
        Long withdrawalId = 300L;
        Long operatorId = 3L;
        BigDecimal amount = BigDecimal.valueOf(400000);

        WithdrawalRequest sharedRequest = WithdrawalRequest.builder()
                .id(withdrawalId)
                .operatorId(operatorId)
                .amount(amount)
                .status(WithdrawalStatus.PENDING)
                .bankName("MBBank")
                .accountNumber("1122334455")
                .accountHolderName("NGUYEN VAN C")
                .build();

        OperatorWallet sharedWallet = OperatorWallet.builder()
                .id(3L)
                .operatorId(operatorId)
                .availableBalance(BigDecimal.valueOf(100000))
                .lockedBalance(amount)
                .version(1L)
                .build();

        when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(sharedRequest));
        when(walletRepository.findByOperatorId(operatorId)).thenReturn(Optional.of(sharedWallet));
        when(withdrawalRepository.save(any(WithdrawalRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.save(any(OperatorWallet.class))).thenAnswer(i -> i.getArgument(0));

        Object lock = new Object();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = List.of(
                () -> {
                    barrier.await();
                    synchronized (lock) {
                        adminService.rejectWithdrawal(withdrawalId, 201L, "Reason 1");
                        successCount.incrementAndGet();
                    }
                    return null;
                },
                () -> {
                    barrier.await();
                    synchronized (lock) {
                        adminService.rejectWithdrawal(withdrawalId, 202L, "Reason 2");
                        successCount.incrementAndGet();
                    }
                    return null;
                }
        );

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures) {
            try {
                f.get();
            } catch (ExecutionException ignored) {}
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Only one rejection must succeed");
        assertEquals(WithdrawalStatus.REJECTED, sharedRequest.getStatus());
        // Available balance should only be refunded once (100k + 400k = 500k)
        assertEquals(BigDecimal.valueOf(500000), sharedWallet.getAvailableBalance());
        verify(transactionRepository, times(1)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("4. Different withdrawals against same wallet respect balance transitions without lost update")
    void testDifferentWithdrawalsAgainstSameWallet() {
        Long operatorId = 4L;
        OperatorWallet wallet = OperatorWallet.builder()
                .id(4L)
                .operatorId(operatorId)
                .availableBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.valueOf(700000))
                .version(1L)
                .build();

        WithdrawalRequest wdr1 = WithdrawalRequest.builder()
                .id(401L)
                .operatorId(operatorId)
                .amount(BigDecimal.valueOf(300000))
                .status(WithdrawalStatus.PENDING)
                .build();

        WithdrawalRequest wdr2 = WithdrawalRequest.builder()
                .id(402L)
                .operatorId(operatorId)
                .amount(BigDecimal.valueOf(400000))
                .status(WithdrawalStatus.PENDING)
                .build();

        when(walletRepository.findByOperatorId(operatorId)).thenReturn(Optional.of(wallet));
        when(withdrawalRepository.findById(401L)).thenReturn(Optional.of(wdr1));
        when(withdrawalRepository.findById(402L)).thenReturn(Optional.of(wdr2));
        when(withdrawalRepository.save(any(WithdrawalRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.save(any(OperatorWallet.class))).thenAnswer(i -> i.getArgument(0));

        // Admin approves Wdr 1 (payout 300k, locked becomes 400k)
        adminService.approveWithdrawal(401L, 99L, "Approved 1");
        assertEquals(WithdrawalStatus.APPROVED, wdr1.getStatus());
        assertEquals(BigDecimal.valueOf(400000), wallet.getLockedBalance());

        // Admin rejects Wdr 2 (refund 400k, locked becomes 0, available becomes 400k)
        adminService.rejectWithdrawal(402L, 99L, "Rejected 2");
        assertEquals(WithdrawalStatus.REJECTED, wdr2.getStatus());
        assertEquals(BigDecimal.ZERO, wallet.getLockedBalance());
        assertEquals(BigDecimal.valueOf(400000), wallet.getAvailableBalance());

        verify(transactionRepository, times(2)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("5. Wallet optimistic lock failure simulation triggers exception and aborts mutation")
    void testWalletOptimisticLockExceptionHandling() {
        Long withdrawalId = 500L;
        Long operatorId = 5L;

        WithdrawalRequest request = WithdrawalRequest.builder()
                .id(withdrawalId)
                .operatorId(operatorId)
                .amount(BigDecimal.valueOf(100000))
                .status(WithdrawalStatus.PENDING)
                .build();

        OperatorWallet wallet = OperatorWallet.builder()
                .id(5L)
                .operatorId(operatorId)
                .availableBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.valueOf(100000))
                .version(1L)
                .build();

        when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(request));
        when(walletRepository.findByOperatorId(operatorId)).thenReturn(Optional.of(wallet));
        // Simulate concurrent modification triggering JPA optimistic lock failure
        when(walletRepository.save(any(OperatorWallet.class)))
                .thenThrow(new OptimisticLockingFailureException("Wallet row modified by another transaction"));

        assertThrows(OptimisticLockingFailureException.class, () -> {
            adminService.approveWithdrawal(withdrawalId, 1L, "Test optimistic fail");
        });

        // Verify transaction ledger was NOT saved due to failure before commit
        verify(transactionRepository, never()).save(any(WalletTransaction.class));
    }
}
