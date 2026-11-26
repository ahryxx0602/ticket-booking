package com.ticket.booking.infrastructure.persistence.repository;

import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.infrastructure.persistence.entity.BookingJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SpringDataBookingRepository extends JpaRepository<BookingJpaEntity, Long> {

    @Query("SELECT DISTINCT b FROM BookingJpaEntity b LEFT JOIN FETCH b.items WHERE b.id = :id")
    Optional<BookingJpaEntity> findByIdWithItems(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BookingJpaEntity b LEFT JOIN FETCH b.items WHERE b.id = :id")
    Optional<BookingJpaEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT DISTINCT b FROM BookingJpaEntity b LEFT JOIN FETCH b.items WHERE b.bookingReference = :bookingReference")
    Optional<BookingJpaEntity> findByBookingReferenceWithItems(@Param("bookingReference") String bookingReference);

    @Query("SELECT DISTINCT b FROM BookingJpaEntity b LEFT JOIN FETCH b.items WHERE b.userId = :userId AND b.clientRequestId = :clientRequestId")
    Optional<BookingJpaEntity> findByUserIdAndClientRequestIdWithItems(@Param("userId") Long userId, @Param("clientRequestId") String clientRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BookingJpaEntity b LEFT JOIN FETCH b.items WHERE b.bookingReference = :bookingReference")
    Optional<BookingJpaEntity> findByBookingReferenceForUpdate(@Param("bookingReference") String bookingReference);

    @Query(value = """
            SELECT i.seat_number
            FROM booking_items i
            INNER JOIN bookings b ON b.id = i.booking_id
            WHERE b.trip_id = :tripId
              AND (b.status = 'CONFIRMED'
                   OR (b.status = 'HELD' AND b.expires_at > :now))
            FOR UPDATE
            """, nativeQuery = true)
    Set<String> findActiveBookedSeatNumbers(@Param("tripId") Long tripId, @Param("now") LocalDateTime now);

    @Query(value = """
            SELECT DISTINCT i.seat_number
            FROM booking_items i
            INNER JOIN bookings b ON b.id = i.booking_id
            WHERE b.trip_id = :tripId
              AND b.status = 'CONFIRMED'
            """, nativeQuery = true)
    Set<String> findConfirmedSeatNumbers(@Param("tripId") Long tripId);

    @Query(value = """
            SELECT DISTINCT i.seat_number
            FROM booking_items i
            INNER JOIN bookings b ON b.id = i.booking_id
            WHERE b.trip_id = :tripId
              AND b.status = 'HELD'
              AND b.expires_at > :now
            """, nativeQuery = true)
    Set<String> findActiveHeldSeatNumbers(@Param("tripId") Long tripId, @Param("now") LocalDateTime now);

    @Query(value = "SELECT b.id FROM BookingJpaEntity b WHERE b.userId = :userId ORDER BY b.createdAt DESC, b.id DESC",
           countQuery = "SELECT COUNT(b.id) FROM BookingJpaEntity b WHERE b.userId = :userId")
    Page<Long> findIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = """
            SELECT b.id FROM BookingJpaEntity b
            WHERE (:status IS NULL OR b.status = :status)
              AND (:tripId IS NULL OR b.tripId = :tripId)
              AND (:userId IS NULL OR b.userId = :userId)
            ORDER BY b.createdAt DESC, b.id DESC
            """,
           countQuery = """
            SELECT COUNT(b.id) FROM BookingJpaEntity b
            WHERE (:status IS NULL OR b.status = :status)
              AND (:tripId IS NULL OR b.tripId = :tripId)
              AND (:userId IS NULL OR b.userId = :userId)
            """)
    Page<Long> findIdsWithFilters(@Param("status") BookingStatus status,
                                  @Param("tripId") Long tripId,
                                  @Param("userId") Long userId,
                                  Pageable pageable);

    @Query("SELECT DISTINCT b FROM BookingJpaEntity b LEFT JOIN FETCH b.items WHERE b.id IN :ids")
    List<BookingJpaEntity> findAllByIdInWithItems(@Param("ids") List<Long> ids);

    @Query("SELECT b.id FROM BookingJpaEntity b WHERE b.status = com.ticket.booking.domain.model.BookingStatus.HELD AND b.expiresAt < :now ORDER BY b.id ASC")
    List<Long> findExpiredHeldBookingIds(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM BookingJpaEntity b " +
           "WHERE b.tripId = :tripId AND b.status = com.ticket.booking.domain.model.BookingStatus.CONFIRMED")
    java.math.BigDecimal sumConfirmedAmountByTripId(@Param("tripId") Long tripId);
}
