package com.ticket.booking.infrastructure.persistence.repository;

import com.ticket.booking.infrastructure.persistence.entity.BookingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataBookingRepository extends JpaRepository<BookingJpaEntity, Long> {
    Optional<BookingJpaEntity> findByBookingReference(String bookingReference);
}
