package com.ticket.catalog.infrastructure.persistence.repository;

import com.ticket.catalog.infrastructure.persistence.entity.SeatJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SpringDataSeatRepository extends JpaRepository<SeatJpaEntity, Long> {
    List<SeatJpaEntity> findByTripIdOrderBySeatNumberAsc(Long tripId);
    boolean existsByTripId(Long tripId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatJpaEntity s WHERE s.tripId = :tripId AND s.seatNumber IN :seatNumbers ORDER BY s.seatNumber ASC")
    List<SeatJpaEntity> findAndLockSeatsByTripIdAndNumbers(@Param("tripId") Long tripId,
                                                           @Param("seatNumbers") Collection<String> seatNumbers);
}
