package com.ticket.partner.application.service;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.application.usecase.PartnerQueryService;
import com.ticket.partner.domain.model.*;
import com.ticket.partner.domain.port.*;
import com.ticket.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnerQueryServiceImpl implements PartnerQueryService {

    public static final BigDecimal DEFAULT_GLOBAL_COMMISSION = BigDecimal.valueOf(10.0);

    private final BusOperatorRepository operatorRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final OperatorWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final BannerRepository bannerRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PartnerCatalogPort partnerCatalogPort;

    @Override
    public com.ticket.shared.dto.PageResponse<PartnerTripResponse> getMyTrips(Long userId, String status, Long vehicleId, int page, int size) {
        BusOperator operator = getOperatorByUserId(userId);
        if (partnerCatalogPort == null) {
            return com.ticket.shared.dto.PageResponse.of(Collections.emptyList(), page, size, 0);
        }
        com.ticket.shared.dto.PageResponse<PartnerTripDto> rawPage = partnerCatalogPort.findOperatorTrips(
                operator.getId(), status, vehicleId, page, size
        );
        List<PartnerTripResponse> items = rawPage.getItems().stream()
                .map(dto -> PartnerTripResponse.builder()
                        .id(dto.getId())
                        .tripCode(dto.getTripCode())
                        .operatorId(dto.getOperatorId())
                        .vehicleId(dto.getVehicleId())
                        .departureStationId(dto.getDepartureStationId())
                        .arrivalStationId(dto.getArrivalStationId())
                        .departureTime(dto.getDepartureTime())
                        .arrivalTime(dto.getArrivalTime())
                        .basePrice(dto.getBasePrice())
                        .status(dto.getStatus())
                        .build())
                .collect(Collectors.toList());
        return com.ticket.shared.dto.PageResponse.of(items, rawPage.getPage(), rawPage.getSize(), rawPage.getTotalElements());
    }

    @Override
    public OperatorResponse getMyOperatorProfile(Long userId) {
        return PartnerMapper.toResponse(getOperatorByUserId(userId));
    }

    @Override
    public List<VehicleResponse> getMyVehicles(Long userId) {
        BusOperator operator = getOperatorByUserId(userId);
        return vehicleRepository.findByOperatorId(operator.getId())
                .stream()
                .map(PartnerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DriverResponse> getMyDrivers(Long userId) {
        BusOperator operator = getOperatorByUserId(userId);
        return driverRepository.findByOperatorId(operator.getId())
                .stream()
                .map(PartnerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WalletResponse getMyWallet(Long userId) {
        BusOperator operator = getOperatorByUserId(userId);
        return walletRepository.findByOperatorId(operator.getId())
                .map(PartnerMapper::toResponse)
                .orElse(null);
    }

    @Override
    public List<WalletTransactionResponse> getMyWalletTransactions(Long userId) {
        BusOperator operator = getOperatorByUserId(userId);
        return transactionRepository.findByOperatorId(operator.getId())
                .stream()
                .map(PartnerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WithdrawalResponse> getMyWithdrawalRequests(Long userId) {
        BusOperator operator = getOperatorByUserId(userId);
        return withdrawalRepository.findByOperatorId(operator.getId())
                .stream()
                .map(PartnerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BannerResponse> getMyBanners(Long userId) {
        BusOperator operator = getOperatorByUserId(userId);
        return bannerRepository.findByOperatorId(operator.getId())
                .stream()
                .map(PartnerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OperatorResponse> getAllOperators() {
        return operatorRepository.findAll()
                .stream()
                .map(PartnerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WithdrawalResponse> getWithdrawalsByStatus(WithdrawalStatus status) {
        List<WithdrawalRequest> list = (status != null)
                ? withdrawalRepository.findByStatus(status)
                : Collections.emptyList();
        return list.stream().map(PartnerMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<BannerResponse> getAllBanners(BannerStatus status) {
        List<Banner> list = (status != null)
                ? bannerRepository.findByStatus(status)
                : bannerRepository.findAll();
        return list.stream().map(PartnerMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<BannerResponse> getActiveBanners(LocalDateTime now, int limit) {
        LocalDateTime queryTime = (now != null) ? now : LocalDateTime.now();
        int queryLimit = (limit > 0) ? limit : 10;
        return bannerRepository.findActiveBanners(queryTime, queryLimit)
                .stream()
                .map(PartnerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getEffectiveCommissionRate(Long operatorId) {
        if (operatorId != null) {
            var opOpt = operatorRepository.findById(operatorId);
            if (opOpt.isPresent() && opOpt.get().getCommissionRate() != null) {
                return opOpt.get().getCommissionRate();
            }
        }
        return getGlobalCommissionRate();
    }

    @Override
    public BigDecimal getGlobalCommissionRate() {
        return systemSettingRepository.findByKey(PartnerAdminServiceImpl.SETTING_COMMISSION_DEFAULT)
                .map(s -> {
                    try {
                        return new BigDecimal(s.getSettingValue());
                    } catch (Exception e) {
                        return DEFAULT_GLOBAL_COMMISSION;
                    }
                })
                .orElse(DEFAULT_GLOBAL_COMMISSION);
    }

    @Override
    public Map<String, String> getAllSystemSettings() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("platformName", systemSettingRepository.findByKey("PLATFORM_NAME")
                .map(SystemSetting::getSettingValue).orElse("VeloBus - Sàn Vé Xe Khách Cao Cấp"));
        settings.put("defaultCommissionRate", getGlobalCommissionRate().toPlainString());
        settings.put("seatHoldTtlMinutes", systemSettingRepository.findByKey("SEAT_HOLD_TTL_MINUTES")
                .map(SystemSetting::getSettingValue).orElse("10"));
        settings.put("turnaroundBufferMinutes", systemSettingRepository.findByKey("TURNAROUND_BUFFER_MINUTES")
                .map(SystemSetting::getSettingValue).orElse("30"));
        settings.put("escrowSettlementDelayHours", systemSettingRepository.findByKey("ESCROW_SETTLEMENT_DELAY_HOURS")
                .map(SystemSetting::getSettingValue).orElse("2"));
        return settings;
    }

    private BusOperator getOperatorByUserId(Long userId) {
        return operatorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND", "Operator profile not found for user: " + userId));
    }
}
