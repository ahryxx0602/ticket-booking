package com.ticket.partner.infrastructure.persistence.adapter;

import com.ticket.partner.domain.model.Vehicle;
import com.ticket.partner.domain.port.VehicleRepository;
import com.ticket.partner.infrastructure.persistence.entity.VehicleJpaEntity;
import com.ticket.partner.infrastructure.persistence.repository.SpringDataVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VehicleRepositoryImpl implements VehicleRepository {

    private final SpringDataVehicleRepository repository;

    @Override
    public Optional<Vehicle> findById(Long id) {
        return repository.findById(id).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public Optional<Vehicle> findByIdAndOperatorId(Long id, Long operatorId) {
        return repository.findByIdAndOperatorId(id, operatorId).map(PartnerEntityMapper::toDomain);
    }

    @Override
    public List<Vehicle> findByOperatorId(Long operatorId) {
        return repository.findByOperatorId(operatorId).stream()
                .map(PartnerEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPlateNumber(String plateNumber) {
        return repository.existsByPlateNumber(plateNumber);
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        VehicleJpaEntity entity = PartnerEntityMapper.toEntity(vehicle);
        VehicleJpaEntity saved = repository.save(entity);
        return PartnerEntityMapper.toDomain(saved);
    }
}
