package com.ticket.partner.application.usecase;

import com.ticket.partner.application.dto.*;

public interface PartnerCommandService {
    OperatorResponse registerOperator(Long userId, RegisterOperatorCommand cmd);
    OperatorResponse updateOperatorProfile(Long userId, UpdateOperatorProfileCommand cmd);
    VehicleResponse createVehicle(Long userId, VehicleCommand cmd);
    VehicleResponse updateVehicle(Long userId, Long vehicleId, VehicleCommand cmd);
    void setVehicleMaintenance(Long userId, Long vehicleId);
    void activateVehicle(Long userId, Long vehicleId);
    DriverResponse createDriver(Long userId, DriverCommand cmd);
    DriverResponse updateDriver(Long userId, Long driverId, DriverCommand cmd);
    WithdrawalResponse requestWithdrawal(Long userId, WithdrawalCommand cmd);
    BannerResponse registerBanner(Long userId, BannerCommand cmd);
    PartnerTripResponse createTrip(Long userId, CreatePartnerTripCommand cmd);
}
