package com.ticket.partner.application;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.application.listener.PartnerPaymentEventListener;
import com.ticket.partner.application.service.PartnerAdminServiceImpl;
import com.ticket.partner.application.service.PartnerCommandServiceImpl;
import com.ticket.partner.application.service.PartnerQueryServiceImpl;
import com.ticket.partner.domain.model.*;
import com.ticket.partner.domain.port.*;
import com.ticket.shared.event.PaymentConfirmedEvent;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Partner Application Layer Unit Tests")
class PartnerApplicationTest {

    @Mock
    private BusOperatorRepository operatorRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DriverRepository driverRepository;
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
    private PartnerCatalogPort partnerCatalogPort;
    @Mock
    private PartnerIdentityPort partnerIdentityPort;

    private PartnerCommandServiceImpl commandService;
    private PartnerAdminServiceImpl adminService;
    private PartnerQueryServiceImpl queryService;
    private PartnerPaymentEventListener paymentEventListener;

    @BeforeEach
    void setUp() {
        commandService = new PartnerCommandServiceImpl(
                operatorRepository, vehicleRepository, driverRepository,
                walletRepository, transactionRepository, withdrawalRepository, bannerRepository,
                partnerCatalogPort
        );
        adminService = new PartnerAdminServiceImpl(
                operatorRepository, walletRepository, transactionRepository,
                withdrawalRepository, bannerRepository, systemSettingRepository, partnerIdentityPort
        );
        queryService = new PartnerQueryServiceImpl(
                operatorRepository, vehicleRepository, driverRepository,
                walletRepository, transactionRepository, withdrawalRepository, bannerRepository, systemSettingRepository,
                partnerCatalogPort
        );
        paymentEventListener = new PartnerPaymentEventListener(
                partnerCatalogPort, queryService, walletRepository, transactionRepository
        );
    }

    @Test
    @DisplayName("Register Operator - Success creates operator and empty wallet")
    void testRegisterOperatorSuccess() {
        RegisterOperatorCommand cmd = RegisterOperatorCommand.builder()
                .businessName("FUTA Express Co")
                .brandName("Phuong Trang")
                .taxCode("0301239999")
                .contactPhone("19006067")
                .contactEmail("contact@futa.vn")
                .build();

        when(operatorRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(operatorRepository.existsByTaxCode("0301239999")).thenReturn(false);

        BusOperator saved = BusOperator.builder()
                .id(1L)
                .userId(10L)
                .businessName(cmd.getBusinessName())
                .brandName(cmd.getBrandName())
                .taxCode(cmd.getTaxCode())
                .status(OperatorStatus.PENDING_APPROVAL)
                .build();
        when(operatorRepository.save(any(BusOperator.class))).thenReturn(saved);

        OperatorResponse response = commandService.registerOperator(10L, cmd);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(OperatorStatus.PENDING_APPROVAL, response.getStatus());

        verify(walletRepository, times(1)).save(any(OperatorWallet.class));
    }

    @Test
    @DisplayName("Register Operator - Fails when duplicate tax code")
    void testRegisterOperatorDuplicateTaxCode() {
        RegisterOperatorCommand cmd = RegisterOperatorCommand.builder()
                .taxCode("0301239999")
                .build();

        when(operatorRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(operatorRepository.existsByTaxCode("0301239999")).thenReturn(true);

        assertThrows(BusinessException.class, () -> commandService.registerOperator(10L, cmd));
    }

    @Test
    @DisplayName("Vehicle - TenantStatusGuard blocks unapproved operators")
    void testCreateVehicleBlocksPendingOperator() {
        BusOperator pendingOp = BusOperator.builder()
                .id(1L)
                .userId(10L)
                .status(OperatorStatus.PENDING_APPROVAL)
                .build();
        when(operatorRepository.findByUserId(10L)).thenReturn(Optional.of(pendingOp));

        VehicleCommand cmd = VehicleCommand.builder()
                .plateNumber("51B-123.45")
                .totalSeats(34)
                .build();

        assertThrows(BusinessException.class, () -> commandService.createVehicle(10L, cmd));
    }

    @Test
    @DisplayName("Withdrawal - Locks available balance into locked balance")
    void testRequestWithdrawalLocksBalance() {
        BusOperator activeOp = BusOperator.builder()
                .id(1L)
                .userId(10L)
                .status(OperatorStatus.ACTIVE)
                .build();
        when(operatorRepository.findByUserId(10L)).thenReturn(Optional.of(activeOp));

        OperatorWallet wallet = OperatorWallet.builder()
                .id(100L)
                .operatorId(1L)
                .availableBalance(BigDecimal.valueOf(1000000))
                .lockedBalance(BigDecimal.ZERO)
                .debtBalance(BigDecimal.ZERO)
                .build();
        when(walletRepository.findByOperatorId(1L)).thenReturn(Optional.of(wallet));

        WithdrawalCommand cmd = WithdrawalCommand.builder()
                .amount(BigDecimal.valueOf(400000))
                .bankName("Vietcombank")
                .accountNumber("123456789")
                .accountHolderName("NGUYEN VAN A")
                .build();

        WithdrawalRequest savedReq = WithdrawalRequest.builder()
                .id(50L)
                .operatorId(1L)
                .amount(cmd.getAmount())
                .status(WithdrawalStatus.PENDING)
                .build();
        when(withdrawalRepository.save(any(WithdrawalRequest.class))).thenReturn(savedReq);

        WithdrawalResponse resp = commandService.requestWithdrawal(10L, cmd);

        assertNotNull(resp);
        assertEquals(BigDecimal.valueOf(600000), wallet.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(400000), wallet.getLockedBalance());
        verify(transactionRepository, times(1)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Admin - Approve Operator grants ROLE_OPERATOR")
    void testAdminApproveOperator() {
        BusOperator operator = BusOperator.builder()
                .id(1L)
                .userId(10L)
                .status(OperatorStatus.PENDING_APPROVAL)
                .build();
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));
        when(operatorRepository.save(any(BusOperator.class))).thenAnswer(i -> i.getArgument(0));

        OperatorResponse resp = adminService.approveOperator(1L, 99L);

        assertEquals(OperatorStatus.ACTIVE, resp.getStatus());
        verify(partnerIdentityPort, times(1)).grantOperatorRole(10L);
    }

    @Test
    @DisplayName("2-Tier Commission - Overrides with custom rate when present")
    void test2TierCommissionResolution() {
        // Case 1: Operator có custom rate 7%
        BusOperator customOp = BusOperator.builder()
                .id(1L)
                .commissionRate(BigDecimal.valueOf(7.0))
                .build();
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(customOp));

        BigDecimal rate1 = queryService.getEffectiveCommissionRate(1L);
        assertEquals(BigDecimal.valueOf(7.0), rate1);

        // Case 2: Operator không có custom rate -> Fallback global rate 12%
        BusOperator defaultOp = BusOperator.builder()
                .id(2L)
                .commissionRate(null)
                .build();
        when(operatorRepository.findById(2L)).thenReturn(Optional.of(defaultOp));
        when(systemSettingRepository.findByKey(PartnerAdminServiceImpl.SETTING_COMMISSION_DEFAULT))
                .thenReturn(Optional.of(SystemSetting.builder().settingValue("12.0").build()));

        BigDecimal rate2 = queryService.getEffectiveCommissionRate(2L);
        assertEquals(BigDecimal.valueOf(12.0), rate2);
    }

    @Test
    @DisplayName("EventListener - PaymentConfirmed credits escrow pending atomic")
    void testPaymentConfirmedCreditsEscrow() {
        Long tripId = 200L;
        Long operatorId = 1L;
        BigDecimal ticketPrice = BigDecimal.valueOf(1000000); // 1,000,000 VND

        when(partnerCatalogPort.findOperatorIdByTripId(tripId)).thenReturn(Optional.of(operatorId));

        BusOperator op = BusOperator.builder()
                .id(operatorId)
                .commissionRate(BigDecimal.valueOf(10.0)) // 10%
                .build();
        when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(op));

        OperatorWallet wallet = OperatorWallet.builder()
                .id(10L)
                .operatorId(operatorId)
                .availableBalance(BigDecimal.ZERO)
                .build();
        when(walletRepository.findByOperatorId(operatorId)).thenReturn(Optional.of(wallet));

        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                "evt-1", "PAY-1", "BKG-1", 101L, 5L, tripId,
                "customer@test.com", "0901234567", "Nguyen Van A",
                ticketPrice, List.of("A01"), Instant.now()
        );

        paymentEventListener.onPaymentConfirmed(event);

        // Net = 1,000,000 - 10% = 900,000 VND
        verify(walletRepository, times(1)).creditPendingAtomic(operatorId, BigDecimal.valueOf(900000));
        verify(transactionRepository, times(1)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Trip Scheduling - Prevents schedule overlap with 30m buffer")
    void testCreateTripWithScheduleOverlapCheck() {
        BusOperator op = BusOperator.builder()
                .id(1L)
                .userId(10L)
                .status(OperatorStatus.ACTIVE)
                .build();
        when(operatorRepository.findByUserId(10L)).thenReturn(Optional.of(op));

        Vehicle v = Vehicle.builder()
                .id(5L)
                .operatorId(1L)
                .plateNumber("51B-123.45")
                .totalSeats(34)
                .status(VehicleStatus.ACTIVE)
                .build();
        when(vehicleRepository.findByIdAndOperatorId(5L, 1L)).thenReturn(Optional.of(v));

        LocalDateTime dep = LocalDateTime.now().plusDays(1);
        LocalDateTime arr = dep.plusHours(4);

        CreatePartnerTripCommand cmd = CreatePartnerTripCommand.builder()
                .tripCode("FUTA-SGN-DL")
                .vehicleId(5L)
                .departureStationId(1L)
                .arrivalStationId(2L)
                .departureTime(dep)
                .arrivalTime(arr)
                .basePrice(BigDecimal.valueOf(250000))
                .build();

        // Case 1: Overlap detected -> Throws BusinessException
        when(partnerCatalogPort.checkVehicleScheduleOverlap(eq(5L), any(), any(), isNull()))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> commandService.createTrip(10L, cmd));

        // Case 2: No overlap -> Success creates trip
        when(partnerCatalogPort.checkVehicleScheduleOverlap(eq(5L), any(), any(), isNull()))
                .thenReturn(false);
        when(partnerCatalogPort.createPartnerTrip(eq(1L), eq(5L), eq("FUTA-SGN-DL"), eq(1L), eq(2L), eq(dep), eq(arr), eq(BigDecimal.valueOf(250000)), eq(34)))
                .thenReturn(999L);

        PartnerTripResponse resp = commandService.createTrip(10L, cmd);
        assertNotNull(resp);
        assertEquals(999L, resp.getId());
        assertEquals("FUTA-SGN-DL", resp.getTripCode());
    }

    @Test
    @DisplayName("FQA-06: Operator Query Service - getMyTrips paginated with ownership filtering")
    void testGetMyTripsPaginated() {
        BusOperator op = BusOperator.builder().id(1L).userId(10L).businessName("FUTA").status(OperatorStatus.ACTIVE).build();
        when(operatorRepository.findByUserId(10L)).thenReturn(Optional.of(op));

        PartnerTripDto tripDto = PartnerTripDto.builder()
                .id(101L)
                .tripCode("FUTA-01")
                .operatorId(1L)
                .vehicleId(5L)
                .departureStationId(1L)
                .arrivalStationId(2L)
                .departureTime(LocalDateTime.now().plusDays(1))
                .arrivalTime(LocalDateTime.now().plusDays(1).plusHours(6))
                .basePrice(BigDecimal.valueOf(300000))
                .status("SCHEDULED")
                .build();

        when(partnerCatalogPort.findOperatorTrips(eq(1L), eq("SCHEDULED"), eq(5L), eq(0), eq(10)))
                .thenReturn(com.ticket.shared.dto.PageResponse.of(List.of(tripDto), 0, 10, 1));

        com.ticket.shared.dto.PageResponse<PartnerTripResponse> pageResp = queryService.getMyTrips(10L, "SCHEDULED", 5L, 0, 10);
        assertNotNull(pageResp);
        assertEquals(1, pageResp.getTotalElements());
        assertEquals(1, pageResp.getItems().size());
        assertEquals("FUTA-01", pageResp.getItems().get(0).getTripCode());
    }
}
