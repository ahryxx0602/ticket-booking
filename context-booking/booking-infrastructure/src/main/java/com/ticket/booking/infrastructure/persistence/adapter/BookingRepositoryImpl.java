package com.ticket.booking.infrastructure.persistence.adapter;

import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingItem;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.booking.infrastructure.persistence.entity.BookingItemJpaEntity;
import com.ticket.booking.infrastructure.persistence.entity.BookingJpaEntity;
import com.ticket.booking.infrastructure.persistence.repository.SpringDataBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingRepositoryImpl implements BookingRepository {

    private final SpringDataBookingRepository springDataBookingRepository;

    @Override
    public Optional<Booking> findById(Long id) {
        return springDataBookingRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Booking> findByReference(String bookingReference) {
        return springDataBookingRepository.findByBookingReference(bookingReference).map(this::toDomain);
    }

    @Override
    public Booking save(Booking booking) {
        BookingJpaEntity entity = toEntity(booking);
        BookingJpaEntity saved = springDataBookingRepository.save(entity);
        return toDomain(saved);
    }

    private Booking toDomain(BookingJpaEntity entity) {
        Booking b = Booking.builder()
                .id(entity.getId())
                .bookingReference(entity.getBookingReference())
                .userId(entity.getUserId())
                .tripId(entity.getTripId())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .expiresAt(entity.getExpiresAt())
                .items(entity.getItems().stream().map(i -> BookingItem.builder()
                        .id(i.getId())
                        .seatId(i.getSeatId())
                        .seatNumber(i.getSeatNumber())
                        .price(i.getPrice())
                        .build()
                ).collect(Collectors.toList()))
                .build();
        b.setCreatedAt(entity.getCreatedAt());
        b.setUpdatedAt(entity.getUpdatedAt());
        return b;
    }

    private BookingJpaEntity toEntity(Booking booking) {
        BookingJpaEntity entity = new BookingJpaEntity();
        entity.setId(booking.getId());
        entity.setBookingReference(booking.getBookingReference());
        entity.setUserId(booking.getUserId());
        entity.setTripId(booking.getTripId());
        entity.setStatus(booking.getStatus());
        entity.setTotalAmount(booking.getTotalAmount());
        entity.setExpiresAt(booking.getExpiresAt());

        if (booking.getItems() != null) {
            entity.setItems(booking.getItems().stream().map(i -> {
                BookingItemJpaEntity itemEntity = new BookingItemJpaEntity();
                itemEntity.setId(i.getId());
                itemEntity.setBooking(entity);
                itemEntity.setSeatId(i.getSeatId() != null ? i.getSeatId() : 0L);
                itemEntity.setSeatNumber(i.getSeatNumber());
                itemEntity.setPrice(i.getPrice());
                return itemEntity;
            }).collect(Collectors.toList()));
        }
        return entity;
    }
}
