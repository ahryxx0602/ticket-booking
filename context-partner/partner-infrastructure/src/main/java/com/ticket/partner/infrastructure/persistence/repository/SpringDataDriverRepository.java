package com.ticket.partner.infrastructure.persistence.repository;

import com.ticket.partner.infrastructure.persistence.entity.DriverJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataDriverRepository extends JpaRepository<DriverJpaEntity, Long> {
    Optional<DriverJpaEntity> findByIdAndOperatorId(Long id, Long operatorId);
    List<DriverJpaEntity> findByOperatorId(Long operatorId);
}
