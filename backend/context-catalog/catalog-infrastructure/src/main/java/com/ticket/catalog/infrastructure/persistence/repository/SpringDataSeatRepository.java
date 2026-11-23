package com.ticket.catalog.infrastructure.persistence.repository;

import com.ticket.catalog.infrastructure.persistence.entity.SeatJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataSeatRepository extends JpaRepository<SeatJpaEntity, Long> {
    List<SeatJpaEntity> findByTripIdOrderBySeatNumberAsc(Long tripId);
    boolean existsByTripId(Long tripId);
}
