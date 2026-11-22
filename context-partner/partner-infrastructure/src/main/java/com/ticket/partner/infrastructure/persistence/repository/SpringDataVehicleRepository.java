package com.ticket.partner.infrastructure.persistence.repository;

import com.ticket.partner.infrastructure.persistence.entity.VehicleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataVehicleRepository extends JpaRepository<VehicleJpaEntity, Long> {
    Optional<VehicleJpaEntity> findByIdAndOperatorId(Long id, Long operatorId);
    List<VehicleJpaEntity> findByOperatorId(Long operatorId);
    boolean existsByPlateNumber(String plateNumber);
}
