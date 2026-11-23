package com.ticket.booking.infrastructure.service;

import com.ticket.booking.application.dto.AdminRefundCommand;
import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.HoldSeatCommand;
import com.ticket.booking.application.dto.RefundResponse;
import com.ticket.booking.application.usecase.AdminRefundBookingUseCase;
import com.ticket.booking.application.usecase.CancelBookingUseCase;
import com.ticket.booking.application.usecase.ConfirmBookingUseCase;
import com.ticket.booking.application.usecase.ExpireHeldBookingsUseCase;
import com.ticket.booking.application.usecase.HoldSeatUseCase;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingItem;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.port.CatalogTripPort;
import com.ticket.booking.domain.port.IdentityUserPort;
import com.ticket.booking.domain.port.PaymentRefundPort;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.booking.domain.repository.SeatLockRepository;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCommandServiceImpl implements HoldSeatUseCase, CancelBookingUseCase, AdminRefundBookingUseCase, ConfirmBookingUseCase, ExpireHeldBookingsUseCase {

    private final BookingRepository bookingRepository;
    private final SeatLockRepository seatLockRepository;
    private final CatalogTripPort catalogTripPort;
    private final IdentityUserPort identityUserPort;
    private final PaymentRefundPort paymentRefundPort;
    private final TransactionTemplate transactionTemplate;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    private static final Duration HOLD_SEAT_DURATION = Duration.ofMinutes(10);

    @Override
    public BookingResponse execute(HoldSeatCommand command, String authenticatedUsername) {
        List<String> normalizedSeats = command.getSeatNumbers().stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList();

        String bookingReference = UUID.randomUUID().toString();
        LocalDateTime holdStart = LocalDateTime.now();

        // 1. Acquire Owner-Token distributed locks OUTSIDE DB transaction to avoid MySQL REPEATABLE READ stale snapshot & Hikari pool exhaustion
        boolean locked = seatLockRepository.acquireLocks(
                command.getTripId(),
                normalizedSeats,
                bookingReference,
                HOLD_SEAT_DURATION
        );

        if (!locked) {
            throw new BusinessException("SEAT_ALREADY_RESERVED", "One or more selected seats are currently held or reserved");
        }

        // 2. Execute short DB transaction; if any validation/conflict/commit fails, release owned locks safely
        try {
            Booking saved = transactionTemplate.execute(status ->
                    createHeldBookingInTransaction(command, authenticatedUsername, normalizedSeats, bookingReference, holdStart)
            );
            return BookingMapper.toResponse(saved);
        } catch (RuntimeException ex) {
            releaseOwnedLocksSafely(command.getTripId(), normalizedSeats, bookingReference);
            throw ex;
        }
    }

    private Booking createHeldBookingInTransaction(HoldSeatCommand command,
                                                   String authenticatedUsername,
                                                   List<String> normalizedSeats,
                                                   String bookingReference,
                                                   LocalDateTime holdStart) {
        IdentityUserPort.AuthenticatedUserInfo user = identityUserPort.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new EntityNotFoundException("User", authenticatedUsername));

        if (user.locked()) {
            throw new BusinessException("ACCOUNT_LOCKED", "Your account has been locked by administrator");
        }

        if (command.getUserId() != null && !command.getUserId().equals(user.userId())) {
            throw new BusinessException("IDOR_VIOLATION", "User ID does not match authenticated user");
        }

        CatalogTripPort.TripSnapshot trip = catalogTripPort.findTripWithSeats(command.getTripId())
                .orElseThrow(() -> new EntityNotFoundException("Trip", command.getTripId()));

        if (!trip.availableForBooking()) {
            throw new BusinessException("TRIP_NOT_AVAILABLE", "Trip is not open for booking");
        }

        List<BookingItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (String seatNumber : normalizedSeats) {
            CatalogTripPort.TripSeatInfo seatInfo = trip.seatsByNumber().get(seatNumber);
            if (seatInfo == null) {
                throw new BusinessException("SEAT_NOT_FOUND", "Seat does not exist on this trip: " + seatNumber);
            }
            items.add(BookingItem.builder()
                    .seatId(seatInfo.seatId())
                    .seatNumber(seatInfo.seatNumber())
                    .price(seatInfo.price())
                    .build());
            totalAmount = totalAmount.add(seatInfo.price());
        }

        Set<String> activeBookedSeats = bookingRepository.findActiveBookedSeatNumbers(command.getTripId(), LocalDateTime.now());
        for (String seatNumber : normalizedSeats) {
            if (activeBookedSeats.contains(seatNumber)) {
                throw new BusinessException("SEAT_ALREADY_RESERVED", "Seat is already held or booked: " + seatNumber);
            }
        }

        Booking booking = Booking.builder()
                .bookingReference(bookingReference)
                .userId(user.userId())
                .phone(user.phone())
                .tripId(command.getTripId())
                .status(BookingStatus.PENDING)
                .totalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP))
                .expiresAt(holdStart.plus(HOLD_SEAT_DURATION))
                .items(items)
                .build();
        booking.transitionTo(BookingStatus.HELD);

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void execute(String bookingReference, String authenticatedUsername) {
        IdentityUserPort.AuthenticatedUserInfo user = identityUserPort.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new EntityNotFoundException("User", authenticatedUsername));

        if (user.locked()) {
            throw new BusinessException("ACCOUNT_LOCKED", "Your account has been locked by administrator");
        }

        Booking booking = bookingRepository.findByReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingReference));

        booking.cancelByCustomer(user.userId(), LocalDateTime.now());
        bookingRepository.save(booking);

        List<String> seats = booking.getItems().stream().map(BookingItem::getSeatNumber).toList();
        releaseLocksAfterCommit(booking.getTripId(), seats, booking.getBookingReference());
    }

    @Override
    @Transactional
    public RefundResponse execute(String bookingReference, AdminRefundCommand command) {
        Booking booking = bookingRepository.findByReferenceForUpdate(bookingReference)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingReference));

        booking.refundByAdmin();
        bookingRepository.save(booking);

        BigDecimal refundAmount = booking.getTotalAmount()
                .multiply(BigDecimal.valueOf(command.getRefundPercentage()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        String trimmedReason = command.getReason().trim();

        boolean paymentRefunded = paymentRefundPort.refundByBookingId(
                booking.getId(),
                refundAmount,
                trimmedReason
        );

        LocalDateTime now = LocalDateTime.now();
        if (!booking.isExpired(now)) {
            List<String> seats = booking.getItems().stream().map(BookingItem::getSeatNumber).toList();
            releaseLocksAfterCommit(booking.getTripId(), seats, booking.getBookingReference());
        }

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.ticket.shared.event.BookingRefundedEvent(
                    UUID.randomUUID().toString(),
                    booking.getId(),
                    booking.getBookingReference(),
                    booking.getTripId(),
                    refundAmount,
                    command.getRefundPercentage(),
                    trimmedReason,
                    java.time.Instant.now()
            ));
        }

        return RefundResponse.builder()
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus())
                .refundPercentage(command.getRefundPercentage())
                .refundAmount(refundAmount)
                .paymentRefunded(paymentRefunded)
                .reason(trimmedReason)
                .refundedAt(now)
                .build();
    }

    private void releaseLocksAfterCommit(Long tripId, List<String> seats, String ownerToken) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    releaseOwnedLocksSafely(tripId, seats, ownerToken);
                }
            });
        } else {
            releaseOwnedLocksSafely(tripId, seats, ownerToken);
        }
    }

    private void releaseOwnedLocksSafely(Long tripId, List<String> seats, String ownerToken) {
        try {
            seatLockRepository.releaseOwnedLocks(tripId, seats, ownerToken);
        } catch (Exception ex) {
            log.warn("Non-fatal error releasing Redis seat locks for tripId={}, token={}: {}", tripId, ownerToken, ex.getMessage());
        }
    }

    @Override
    public boolean execute(Long bookingId, LocalDateTime now) {
        TransactionTemplate template = this.transactionTemplate;
        if (template != null && template.getTransactionManager() != null) {
            TransactionTemplate requiresNew = new TransactionTemplate(template.getTransactionManager());
            requiresNew.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            template = requiresNew;
        }
        try {
            return Boolean.TRUE.equals(template.execute(status -> {
                Booking booking = bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new EntityNotFoundException("Booking", bookingId));
                if (booking.getStatus() == BookingStatus.CONFIRMED) {
                    return true;
                }
                if (booking.getStatus() != BookingStatus.HELD) {
                    log.warn("Cannot confirm booking {} because status is {}", bookingId, booking.getStatus());
                    return false;
                }
                if (booking.isExpired(now)) {
                    log.warn("Cannot confirm booking {} because hold time expired at {}", bookingId, booking.getExpiresAt());
                    return false;
                }
                booking.confirm(now);
                bookingRepository.save(booking);
                log.info("Booking {} confirmed successfully", booking.getBookingReference());
                return true;
            }));
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic locking conflict confirming booking {}: {}", bookingId, ex.getMessage());
            return false;
        }
    }

    @Override
    public int execute(LocalDateTime now) {
        List<Booking> expiredBookings = bookingRepository.findExpiredHeldBookings(now, 100);
        if (expiredBookings.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Booking booking : expiredBookings) {
            boolean success = expireSingleBooking(booking.getId(), now);
            if (success) {
                count++;
            }
        }
        return count;
    }

    public boolean expireSingleBooking(Long bookingId, LocalDateTime now) {
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                Booking booking = bookingRepository.findById(bookingId).orElse(null);
                if (booking == null || booking.getStatus() != BookingStatus.HELD || !booking.isExpired(now)) {
                    return false;
                }
                booking.expire(now);
                bookingRepository.save(booking);

                List<String> seats = booking.getItems().stream()
                        .map(BookingItem::getSeatNumber)
                        .toList();
                Long tripId = booking.getTripId();
                String token = booking.getBookingReference();

                releaseLocksAfterCommit(tripId, seats, token);
                if (eventPublisher != null) {
                    eventPublisher.publishEvent(new com.ticket.shared.event.BookingExpiredEvent(
                            UUID.randomUUID().toString(),
                            token,
                            booking.getId(),
                            booking.getUserId(),
                            null,
                            booking.getPhone(),
                            java.time.Instant.now()
                    ));
                }
                log.info("Booking {} transitioned to EXPIRED by scheduler", token);
                return true;
            }));
        } catch (Exception e) {
            log.warn("Failed to expire booking {}: {}", bookingId, e.getMessage());
            return false;
        }
    }
}
