package com.ticket.partner.domain;

import com.ticket.partner.domain.model.*;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Partner Domain Model & Invariant Unit Tests")
class PartnerDomainTest {

    @Test
    @DisplayName("BusOperator - State transitions and commission rate update")
    void testBusOperatorStateTransitions() {
        BusOperator operator = BusOperator.builder()
                .id(1L)
                .userId(10L)
                .businessName("Cong ty TNHH Van Tai Phuong Trang")
                .brandName("FUTA Bus Lines")
                .taxCode("0301234567")
                .contactPhone("19006067")
                .contactEmail("hotro@futa.vn")
                .status(OperatorStatus.PENDING_APPROVAL)
                .build();

        assertEquals(OperatorStatus.PENDING_APPROVAL, operator.getStatus());

        // Update commission rate valid (5.5%)
        operator.updateCommissionRate(BigDecimal.valueOf(5.5));
        assertEquals(BigDecimal.valueOf(5.5), operator.getCommissionRate());

        // Invalid commission rate > 100% or < 0%
        assertThrows(BusinessException.class, () -> operator.updateCommissionRate(BigDecimal.valueOf(105)));
        assertThrows(BusinessException.class, () -> operator.updateCommissionRate(BigDecimal.valueOf(-1)));

        // Activate
        operator.activate();
        assertEquals(OperatorStatus.ACTIVE, operator.getStatus());

        // Suspend
        operator.suspend("Vi pham quy che chat luong dich vu");
        assertEquals(OperatorStatus.SUSPENDED, operator.getStatus());
        assertEquals("Vi pham quy che chat luong dich vu", operator.getRejectionReason());

        // Rejection should only work when PENDING_APPROVAL
        assertThrows(BusinessException.class, () -> operator.reject("Cannot reject when suspended"));

        // Update profile
        operator.updateProfile("FUTA Express", "19006068", "info@futa.vn", "https://img.com/logo.png", "Mota moi");
        assertEquals("FUTA Express", operator.getBrandName());
        assertEquals("19006068", operator.getContactPhone());
    }

    @Test
    @DisplayName("Vehicle - Validation and maintenance state toggles")
    void testVehicleValidationAndMaintenance() {
        Vehicle vehicle = Vehicle.builder()
                .operatorId(1L)
                .plateNumber("51B-123.45")
                .modelName("Thaco Mobihome 2024")
                .vehicleType(VehicleType.SLEEPER)
                .totalSeats(34)
                .status(VehicleStatus.ACTIVE)
                .build();

        assertDoesNotThrow(vehicle::validate);

        vehicle.setMaintenance();
        assertEquals(VehicleStatus.MAINTENANCE, vehicle.getStatus());

        vehicle.activate();
        assertEquals(VehicleStatus.ACTIVE, vehicle.getStatus());

        // Blank plate number
        Vehicle invalidPlate = Vehicle.builder()
                .operatorId(1L)
                .plateNumber("   ")
                .totalSeats(34)
                .build();
        assertThrows(BusinessException.class, invalidPlate::validate);

        // Invalid seats
        Vehicle invalidSeats = Vehicle.builder()
                .operatorId(1L)
                .plateNumber("51B-999.99")
                .totalSeats(0)
                .build();
        assertThrows(BusinessException.class, invalidSeats::validate);
    }

    @Test
    @DisplayName("OperatorWallet - Escrow credit, settlement and automatic debt recovery")
    void testOperatorWalletSettlementAndDebtRecovery() {
        OperatorWallet wallet = OperatorWallet.builder()
                .operatorId(1L)
                .availableBalance(BigDecimal.ZERO)
                .pendingSettlementBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .debtBalance(BigDecimal.ZERO)
                .build();

        // 1. Credit pending balance from booking
        wallet.creditPending(BigDecimal.valueOf(900000));
        assertEquals(BigDecimal.valueOf(900000), wallet.getPendingSettlementBalance());
        assertEquals(BigDecimal.ZERO, wallet.getAvailableBalance());

        // 2. Settle 900,000 to available
        wallet.settleToAvailable(BigDecimal.valueOf(900000));
        assertEquals(BigDecimal.ZERO, wallet.getPendingSettlementBalance());
        assertEquals(BigDecimal.valueOf(900000), wallet.getAvailableBalance());

        // 3. Simulate refund when balance is empty -> generates debt
        wallet.setAvailableBalance(BigDecimal.valueOf(100000));
        wallet.refundDeduction(BigDecimal.valueOf(300000), false); // Needs 300k, only has 100k -> 200k debt
        assertEquals(BigDecimal.ZERO, wallet.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(200000), wallet.getDebtBalance());

        // 4. Next settlement of 500,000 should automatically clear 200,000 debt first!
        wallet.creditPending(BigDecimal.valueOf(500000));
        wallet.settleToAvailable(BigDecimal.valueOf(500000));
        assertEquals(BigDecimal.ZERO, wallet.getDebtBalance());
        assertEquals(BigDecimal.valueOf(300000), wallet.getAvailableBalance()); // 500k - 200k debt = 300k
    }

    @Test
    @DisplayName("OperatorWallet - Withdrawal locking, debiting and releasing")
    void testOperatorWalletWithdrawalFlow() {
        OperatorWallet wallet = OperatorWallet.builder()
                .operatorId(1L)
                .availableBalance(BigDecimal.valueOf(1000000))
                .pendingSettlementBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .debtBalance(BigDecimal.ZERO)
                .build();

        // Lock 400,000 for withdrawal
        wallet.lockForWithdrawal(BigDecimal.valueOf(400000));
        assertEquals(BigDecimal.valueOf(600000), wallet.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(400000), wallet.getLockedBalance());

        // Rejection releases locked balance back to available
        wallet.releaseLocked(BigDecimal.valueOf(400000));
        assertEquals(BigDecimal.valueOf(1000000), wallet.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, wallet.getLockedBalance());

        // Re-lock and approve (debit locked)
        wallet.lockForWithdrawal(BigDecimal.valueOf(500000));
        assertEquals(BigDecimal.valueOf(500000), wallet.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(500000), wallet.getLockedBalance());

        wallet.debitLocked(BigDecimal.valueOf(500000));
        assertEquals(BigDecimal.valueOf(500000), wallet.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, wallet.getLockedBalance());

        // Cannot withdraw more than available
        assertThrows(BusinessException.class, () -> wallet.lockForWithdrawal(BigDecimal.valueOf(600000)));

        // Cannot withdraw if debt exists
        wallet.setDebtBalance(BigDecimal.valueOf(50000));
        assertThrows(BusinessException.class, () -> wallet.lockForWithdrawal(BigDecimal.valueOf(100000)));
    }

    @Test
    @DisplayName("WithdrawalRequest - Minimum amount validation and approval states")
    void testWithdrawalRequestValidationAndApproval() {
        WithdrawalRequest request = WithdrawalRequest.builder()
                .operatorId(1L)
                .amount(BigDecimal.valueOf(500000))
                .bankName("Vietcombank")
                .accountNumber("0123456789")
                .accountHolderName("NGUYEN VAN A")
                .status(WithdrawalStatus.PENDING)
                .build();

        assertDoesNotThrow(request::validate);

        // Less than 100,000 VND should fail
        WithdrawalRequest lowAmount = WithdrawalRequest.builder()
                .operatorId(1L)
                .amount(BigDecimal.valueOf(99000))
                .bankName("Vietcombank")
                .accountNumber("0123456789")
                .accountHolderName("NGUYEN VAN A")
                .build();
        assertThrows(BusinessException.class, lowAmount::validate);

        // Approve
        request.approve(99L, "Chuyen khoan thanh cong Ref #99281");
        assertEquals(WithdrawalStatus.APPROVED, request.getStatus());
        assertEquals(99L, request.getProcessedByAdminId());

        // Already approved cannot be rejected
        assertThrows(BusinessException.class, () -> request.reject(99L, "Cannot reject"));
    }

    @Test
    @DisplayName("Banner - XSS sanitization and date validation")
    void testBannerValidationAndSanitization() {
        Banner banner = Banner.builder()
                .operatorId(1L)
                .title("Khuyen mai Tet Giap Thin 2026")
                .imageUrl("https://cdn.vexere.com/banners/tet2026.png")
                .targetUrl("https://vexere.com/khuyen-mai-tet")
                .position(BannerPosition.HOMEPAGE_HERO)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(10))
                .status(BannerStatus.PENDING_APPROVAL)
                .build();

        assertDoesNotThrow(banner::validate);

        // XSS script injection in target URL
        Banner xssBanner = Banner.builder()
                .title("XSS Hack")
                .imageUrl("https://cdn.vexere.com/xss.png")
                .targetUrl("javascript:alert('pwned')")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        assertThrows(BusinessException.class, xssBanner::validate);

        // Invalid dates (end date before start date)
        Banner invalidDates = Banner.builder()
                .title("Invalid Dates")
                .imageUrl("https://cdn.vexere.com/test.png")
                .startDate(LocalDateTime.now().plusDays(5))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        assertThrows(BusinessException.class, invalidDates::validate);

        // Approve / Reject
        banner.approve();
        assertEquals(BannerStatus.ACTIVE, banner.getStatus());

        banner.reject("Hinh anh khong dung kich thuoc quy dinh");
        assertEquals(BannerStatus.REJECTED, banner.getStatus());
        assertEquals("Hinh anh khong dung kich thuoc quy dinh", banner.getRejectionReason());
    }
}
