package com.ticket.booking.infrastructure.persistence.adapter;

import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingItem;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.booking.infrastructure.persistence.entity.BookingItemJpaEntity;
import com.ticket.booking.infrastructure.persistence.entity.BookingJpaEntity;
import com.ticket.booking.infrastructure.persistence.repository.SpringDataBookingRepository;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingRepositoryImpl implements BookingRepository {

    private final SpringDataBookingRepository springDataBookingRepository;

    @Override
    public Optional<Booking> findById(Long id) {
        return springDataBookingRepository.findByIdWithItems(id).map(this::toDomain);
    }

    @Override
    public Optional<Booking> findByIdForUpdate(Long id) {
        return springDataBookingRepository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public Optional<Booking> findByReference(String bookingReference) {
        return springDataBookingRepository.findByBookingReferenceWithItems(bookingReference).map(this::toDomain);
    }

    @Override
    public Optional<Booking> findByUserIdAndClientRequestId(Long userId, String clientRequestId) {
        if (userId == null || clientRequestId == null || clientRequestId.isBlank()) {
            return Optional.empty();
        }
        return springDataBookingRepository.findByUserIdAndClientRequestIdWithItems(userId, clientRequestId).map(this::toDomain);
    }

    @Override
    public Optional<Booking> findByReferenceForUpdate(String bookingReference) {
        return springDataBookingRepository.findByBookingReferenceForUpdate(bookingReference).map(this::toDomain);
    }

    @Override
    public Set<String> findActiveBookedSeatNumbers(Long tripId, LocalDateTime now) {
        return springDataBookingRepository.findActiveBookedSeatNumbers(tripId, now);
    }

    @Override
    public Set<String> findConfirmedSeatNumbers(Long tripId) {
        return springDataBookingRepository.findConfirmedSeatNumbers(tripId);
    }

    @Override
    public Set<String> findActiveHeldSeatNumbers(Long tripId, LocalDateTime now) {
        return springDataBookingRepository.findActiveHeldSeatNumbers(tripId, now);
    }

    @Override
    public PageResponse<Booking> findByUserId(Long userId, int page, int size) {
        Page<Long> idPage = springDataBookingRepository.findIdsByUserId(userId, PageRequest.of(page, size));
        return toPageResponse(idPage);
    }

    @Override
    public PageResponse<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, int page, int size) {
        Page<Long> idPage = springDataBookingRepository.findIdsByUserIdAndStatus(userId, status, PageRequest.of(page, size));
        return toPageResponse(idPage);
    }

    @Override
    public PageResponse<Booking> findAllWithFilters(BookingStatus status, Long tripId, Long userId, int page, int size) {
        Page<Long> idPage = springDataBookingRepository.findIdsWithFilters(status, tripId, userId, PageRequest.of(page, size));
        return toPageResponse(idPage);
    }

    @Override
    public List<Booking> findExpiredHeldBookings(LocalDateTime now, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<Long> ids = springDataBookingRepository.findExpiredHeldBookingIds(now, PageRequest.of(0, safeLimit));
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, BookingJpaEntity> byId = springDataBookingRepository.findAllByIdInWithItems(ids).stream()
                .collect(Collectors.toMap(BookingJpaEntity::getId, Function.identity()));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Booking save(Booking booking) {
        BookingJpaEntity entity;
        if (booking.getId() != null) {
            entity = springDataBookingRepository.findByIdWithItems(booking.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Booking", booking.getId()));
            if (booking.getVersion() != null && !booking.getVersion().equals(entity.getVersion())) {
                throw new ObjectOptimisticLockingFailureException(BookingJpaEntity.class, booking.getId());
            }
            entity.setStatus(booking.getStatus());
            entity.setTotalAmount(booking.getTotalAmount());
            entity.setExpiresAt(booking.getExpiresAt());
            if (booking.getClientRequestId() != null) {
                entity.setClientRequestId(booking.getClientRequestId());
            }
        } else {
            entity = toNewEntity(booking);
        }
        BookingJpaEntity saved = springDataBookingRepository.save(entity);
        return toDomain(saved);
    }

    private PageResponse<Booking> toPageResponse(Page<Long> idPage) {
        List<Long> ids = idPage.getContent();
        List<Booking> items = Collections.emptyList();
        if (!ids.isEmpty()) {
            Map<Long, BookingJpaEntity> byId = springDataBookingRepository.findAllByIdInWithItems(ids).stream()
                    .collect(Collectors.toMap(BookingJpaEntity::getId, Function.identity()));
            items = ids.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .map(this::toDomain)
                    .toList();
        }
        return PageResponse.<Booking>builder()
                .items(items)
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .hasNext(idPage.hasNext())
                .hasPrevious(idPage.hasPrevious())
                .build();
    }

    private Booking toDomain(BookingJpaEntity entity) {
        Booking b = Booking.builder()
                .id(entity.getId())
                .bookingReference(entity.getBookingReference())
                .userId(entity.getUserId())
                .phone(entity.getPhone())
                .tripId(entity.getTripId())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .expiresAt(entity.getExpiresAt())
                .version(entity.getVersion())
                .clientRequestId(entity.getClientRequestId())
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

    private BookingJpaEntity toNewEntity(Booking booking) {
        BookingJpaEntity entity = new BookingJpaEntity();
        entity.setBookingReference(booking.getBookingReference());
        entity.setUserId(booking.getUserId());
        entity.setPhone(booking.getPhone());
        entity.setTripId(booking.getTripId());
        entity.setStatus(booking.getStatus());
        entity.setTotalAmount(booking.getTotalAmount());
        entity.setExpiresAt(booking.getExpiresAt());
        entity.setVersion(booking.getVersion() != null ? booking.getVersion() : 0L);
        entity.setClientRequestId(booking.getClientRequestId());

        if (booking.getItems() != null) {
            entity.setItems(booking.getItems().stream().map(i -> {
                if (i.getSeatId() == null || i.getSeatId() <= 0) {
                    throw new IllegalStateException("BookingItem seatId must be a valid positive seat ID");
                }
                BookingItemJpaEntity itemEntity = new BookingItemJpaEntity();
                itemEntity.setId(i.getId());
                itemEntity.setBooking(entity);
                itemEntity.setSeatId(i.getSeatId());
                itemEntity.setSeatNumber(i.getSeatNumber());
                itemEntity.setPrice(i.getPrice());
                return itemEntity;
            }).collect(Collectors.toList()));
        }
        return entity;
    }
}
