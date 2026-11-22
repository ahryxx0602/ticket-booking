package com.ticket.catalog.infrastructure.persistence.repository;

import com.ticket.catalog.infrastructure.persistence.entity.TripJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataTripRepository extends JpaRepository<TripJpaEntity, Long> {

    boolean existsByTripCode(String tripCode);

    @Query("SELECT t FROM TripJpaEntity t " +
           "JOIN FETCH t.departureStation " +
           "JOIN FETCH t.arrivalStation " +
           "WHERE t.id = :id")
    Optional<TripJpaEntity> findByIdWithStations(@Param("id") Long id);

    @Query("SELECT t FROM TripJpaEntity t " +
           "JOIN FETCH t.departureStation " +
           "JOIN FETCH t.arrivalStation " +
           "WHERE t.tripCode = :tripCode")
    Optional<TripJpaEntity> findByTripCodeWithStations(@Param("tripCode") String tripCode);

    @Query("SELECT t FROM TripJpaEntity t " +
           "JOIN FETCH t.departureStation ds " +
           "JOIN FETCH t.arrivalStation as " +
           "WHERE (:depCity IS NULL OR ds.city = :depCity) " +
           "AND (:arrCity IS NULL OR as.city = :arrCity) " +
           "AND t.status = com.ticket.catalog.domain.model.TripStatus.SCHEDULED " +
           "AND t.departureTime >= :fromTime AND t.departureTime <= :toTime")
    List<TripJpaEntity> searchTrips(
            @Param("depCity") String depCity,
            @Param("arrCity") String arrCity,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );
}
