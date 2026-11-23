package com.ticket.partner.infrastructure.persistence.repository;

import com.ticket.partner.domain.model.OperatorStatus;
import com.ticket.partner.infrastructure.persistence.entity.BusOperatorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataBusOperatorRepository extends JpaRepository<BusOperatorJpaEntity, Long> {
    Optional<BusOperatorJpaEntity> findByUserId(Long userId);
    List<BusOperatorJpaEntity> findByStatus(OperatorStatus status);
    boolean existsByTaxCode(String taxCode);
}
