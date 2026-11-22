package com.ticket.partner.application.service;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.application.usecase.PartnerCommandService;
import com.ticket.partner.domain.model.*;
import com.ticket.partner.domain.port.*;
import com.ticket.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerCommandServiceImpl implements PartnerCommandService {

    private final BusOperatorRepository operatorRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final OperatorWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final BannerRepository bannerRepository;
    private final PartnerCatalogPort partnerCatalogPort;

    @Override
    @Transactional
    public OperatorResponse registerOperator(Long userId, RegisterOperatorCommand cmd) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "User identity is required");
        }
        if (operatorRepository.findByUserId(userId).isPresent()) {
            throw new BusinessException("OPERATOR_ALREADY_EXISTS", "User has already registered as a bus operator");
        }
        if (operatorRepository.existsByTaxCode(cmd.getTaxCode())) {
            throw new BusinessException("TAX_CODE_EXISTS", "Tax code is already registered by another operator: " + cmd.getTaxCode());
        }

        BusOperator operator = BusOperator.builder()
                .userId(userId)
                .businessName(cmd.getBusinessName())
                .brandName(cmd.getBrandName())
                .taxCode(cmd.getTaxCode())
                .contactPhone(cmd.getContactPhone())
                .contactEmail(cmd.getContactEmail())
                .logoUrl(cmd.getLogoUrl())
                .description(cmd.getDescription())
                .status(OperatorStatus.PENDING_APPROVAL)
                .build();

        BusOperator saved = operatorRepository.save(operator);

        // Tạo sẵn ví ký quỹ rỗng cho nhà xe
        OperatorWallet wallet = OperatorWallet.builder()
                .operatorId(saved.getId())
                .availableBalance(BigDecimal.ZERO)
                .pendingSettlementBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .debtBalance(BigDecimal.ZERO)
                .currency("VND")
                .build();
        walletRepository.save(wallet);

        log.info("Registered new bus operator candidate: ID={}, Business={}, User={}", saved.getId(), saved.getBusinessName(), userId);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OperatorResponse updateOperatorProfile(Long userId, UpdateOperatorProfileCommand cmd) {
        BusOperator operator = getOperatorByUserId(userId);
        operator.updateProfile(cmd.getBrandName(), cmd.getContactPhone(), cmd.getContactEmail(), cmd.getLogoUrl(), cmd.getDescription());
        BusOperator saved = operatorRepository.save(operator);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(Long userId, VehicleCommand cmd) {
        BusOperator operator = getActiveOperatorByUserId(userId);
        if (vehicleRepository.existsByPlateNumber(cmd.getPlateNumber())) {
            throw new BusinessException("PLATE_NUMBER_EXISTS", "Plate number already registered: " + cmd.getPlateNumber());
        }

        Vehicle vehicle = Vehicle.builder()
                .operatorId(operator.getId())
                .plateNumber(cmd.getPlateNumber())
                .modelName(cmd.getModelName())
                .vehicleType(cmd.getVehicleType())
                .totalSeats(cmd.getTotalSeats())
                .seatRows(cmd.getSeatRows() != null ? cmd.getSeatRows() : 10)
                .seatsPerRow(cmd.getSeatsPerRow() != null ? cmd.getSeatsPerRow() : 4)
                .vipRows(cmd.getVipRows() != null ? cmd.getVipRows() : 2)
                .status(VehicleStatus.ACTIVE)
                .build();
        vehicle.validate();

        Vehicle saved = vehicleRepository.save(vehicle);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long userId, Long vehicleId, VehicleCommand cmd) {
        BusOperator operator = getActiveOperatorByUserId(userId);
        Vehicle vehicle = vehicleRepository.findByIdAndOperatorId(vehicleId, operator.getId())
                .orElseThrow(() -> new BusinessException("VEHICLE_NOT_FOUND", "Vehicle not found or does not belong to operator"));

        if (!vehicle.getPlateNumber().equalsIgnoreCase(cmd.getPlateNumber()) && vehicleRepository.existsByPlateNumber(cmd.getPlateNumber())) {
            throw new BusinessException("PLATE_NUMBER_EXISTS", "Plate number already registered: " + cmd.getPlateNumber());
        }

        vehicle.setPlateNumber(cmd.getPlateNumber());
        vehicle.setModelName(cmd.getModelName());
        vehicle.setVehicleType(cmd.getVehicleType());
        vehicle.setTotalSeats(cmd.getTotalSeats());
        if (cmd.getSeatRows() != null) vehicle.setSeatRows(cmd.getSeatRows());
        if (cmd.getSeatsPerRow() != null) vehicle.setSeatsPerRow(cmd.getSeatsPerRow());
        if (cmd.getVipRows() != null) vehicle.setVipRows(cmd.getVipRows());
        vehicle.validate();

        Vehicle saved = vehicleRepository.save(vehicle);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void setVehicleMaintenance(Long userId, Long vehicleId) {
        BusOperator operator = getActiveOperatorByUserId(userId);
        Vehicle vehicle = vehicleRepository.findByIdAndOperatorId(vehicleId, operator.getId())
                .orElseThrow(() -> new BusinessException("VEHICLE_NOT_FOUND", "Vehicle not found"));
        vehicle.setMaintenance();
        vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public void activateVehicle(Long userId, Long vehicleId) {
        BusOperator operator = getActiveOperatorByUserId(userId);
        Vehicle vehicle = vehicleRepository.findByIdAndOperatorId(vehicleId, operator.getId())
                .orElseThrow(() -> new BusinessException("VEHICLE_NOT_FOUND", "Vehicle not found"));
        vehicle.activate();
        vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public DriverResponse createDriver(Long userId, DriverCommand cmd) {
        BusOperator operator = getActiveOperatorByUserId(userId);
        Driver driver = Driver.builder()
                .operatorId(operator.getId())
                .fullName(cmd.getFullName())
                .phone(cmd.getPhone())
                .licenseNumber(cmd.getLicenseNumber())
                .status(DriverStatus.ACTIVE)
                .build();
        driver.validate();

        Driver saved = driverRepository.save(driver);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DriverResponse updateDriver(Long userId, Long driverId, DriverCommand cmd) {
        BusOperator operator = getActiveOperatorByUserId(userId);
        Driver driver = driverRepository.findByIdAndOperatorId(driverId, operator.getId())
                .orElseThrow(() -> new BusinessException("DRIVER_NOT_FOUND", "Driver not found"));

        driver.setFullName(cmd.getFullName());
        driver.setPhone(cmd.getPhone());
        driver.setLicenseNumber(cmd.getLicenseNumber());
        driver.validate();

        Driver saved = driverRepository.save(driver);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WithdrawalResponse requestWithdrawal(Long userId, WithdrawalCommand cmd) {
        BusOperator operator = getActiveOperatorByUserId(userId);
        OperatorWallet wallet = walletRepository.findByOperatorId(operator.getId())
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Operator wallet not found"));

        // Domain invariant: Kiểm tra số dư và chuyển sang locked_balance (ADR-13)
        wallet.lockForWithdrawal(cmd.getAmount());
        walletRepository.save(wallet);

        WithdrawalRequest request = WithdrawalRequest.builder()
                .operatorId(operator.getId())
                .amount(cmd.getAmount())
                .bankName(cmd.getBankName())
                .accountNumber(cmd.getAccountNumber())
                .accountHolderName(cmd.getAccountHolderName())
                .status(WithdrawalStatus.PENDING)
                .build();
        request.validate();
        WithdrawalRequest savedRequest = withdrawalRepository.save(request);

        // Audit transaction
        WalletTransaction tx = WalletTransaction.builder()
                .walletId(wallet.getId())
                .operatorId(operator.getId())
                .transactionType(WalletTransactionType.WITHDRAWAL_LOCK)
                .netAmount(cmd.getAmount().negate())
                .balanceBefore(wallet.getAvailableBalance().add(cmd.getAmount()))
                .balanceAfter(wallet.getAvailableBalance())
                .description("Lock balance for withdrawal request #" + savedRequest.getId())
                .build();
        transactionRepository.save(tx);

        log.info("Operator ID={} requested withdrawal #{} amount={}", operator.getId(), savedRequest.getId(), cmd.getAmount());
        return PartnerMapper.toResponse(savedRequest);
    }

    @Override
    @Transactional
    public BannerResponse registerBanner(Long userId, BannerCommand cmd) {
        BusOperator operator = getActiveOperatorByUserId(userId);
        Banner banner = Banner.builder()
                .operatorId(operator.getId())
                .title(cmd.getTitle())
                .imageUrl(cmd.getImageUrl())
                .targetUrl(cmd.getTargetUrl())
                .position(cmd.getPosition())
                .startDate(cmd.getStartDate())
                .endDate(cmd.getEndDate())
                .priority(cmd.getPriority() != null ? cmd.getPriority() : 0)
                .status(BannerStatus.PENDING_APPROVAL)
                .build();
        banner.validate();

        Banner saved = bannerRepository.save(banner);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PartnerTripResponse createTrip(Long userId, CreatePartnerTripCommand cmd) {
        BusOperator operator = getActiveOperatorByUserId(userId);

        Vehicle vehicle = vehicleRepository.findByIdAndOperatorId(cmd.getVehicleId(), operator.getId())
                .orElseThrow(() -> new BusinessException("VEHICLE_NOT_FOUND", "Vehicle not found or does not belong to operator"));

        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new BusinessException("VEHICLE_NOT_ACTIVE", "Vehicle is not in ACTIVE status: " + vehicle.getStatus());
        }

        if (cmd.getDepartureTime() == null || cmd.getArrivalTime() == null || !cmd.getArrivalTime().isAfter(cmd.getDepartureTime())) {
            throw new BusinessException("INVALID_TRIP_TIME", "Arrival time must be strictly after departure time");
        }

        // Chốt chặn chống trùng lịch chạy xe với đệm xoay vòng 30 phút (Turnaround buffer 30m)
        boolean overlap = partnerCatalogPort.checkVehicleScheduleOverlap(
                vehicle.getId(),
                cmd.getDepartureTime().minusMinutes(30),
                cmd.getArrivalTime().plusMinutes(30),
                null
        );
        if (overlap) {
            throw new BusinessException("SCHEDULE_OVERLAP", "Vehicle schedule overlaps with existing trip (30-min turnaround buffer required)");
        }

        Long tripId = partnerCatalogPort.createPartnerTrip(
                operator.getId(),
                vehicle.getId(),
                cmd.getTripCode(),
                cmd.getDepartureStationId(),
                cmd.getArrivalStationId(),
                cmd.getDepartureTime(),
                cmd.getArrivalTime(),
                cmd.getBasePrice(),
                vehicle.getTotalSeats()
        );

        log.info("Operator ID={} successfully created trip ID={}, Code={}, Vehicle={}",
                operator.getId(), tripId, cmd.getTripCode(), vehicle.getPlateNumber());

        return PartnerTripResponse.builder()
                .id(tripId)
                .tripCode(cmd.getTripCode())
                .operatorId(operator.getId())
                .vehicleId(vehicle.getId())
                .departureStationId(cmd.getDepartureStationId())
                .arrivalStationId(cmd.getArrivalStationId())
                .departureTime(cmd.getDepartureTime())
                .arrivalTime(cmd.getArrivalTime())
                .basePrice(cmd.getBasePrice())
                .status("SCHEDULED")
                .build();
    }

    private BusOperator getOperatorByUserId(Long userId) {
        return operatorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND", "Bus operator profile not found for user: " + userId));
    }

    private BusOperator getActiveOperatorByUserId(Long userId) {
        BusOperator operator = getOperatorByUserId(userId);
        if (operator.getStatus() != OperatorStatus.ACTIVE) {
            throw new BusinessException("OPERATOR_NOT_ACTIVE", "Bus operator account is not active. Status: " + operator.getStatus());
        }
        return operator;
    }
}
