package com.ticket.partner.domain.port;

import com.ticket.partner.domain.model.Vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    Optional<Vehicle> findById(Long id);
    Optional<Vehicle> findByIdAndOperatorId(Long id, Long operatorId);
    List<Vehicle> findByOperatorId(Long operatorId);
    boolean existsByPlateNumber(String plateNumber);
    Vehicle save(Vehicle vehicle);
}
