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

        String clientRequestId = command.getClientRequestId() != null && !command.getClientRequestId().isBlank()
                ? command.getClientRequestId().trim()
                : null;

        IdentityUserPort.AuthenticatedUserInfo user = null;
        if (clientRequestId != null) {
            user = identityUserPort.findByUsername(authenticatedUsername)
                    .orElseThrow(() -> new EntityNotFoundException("User", authenticatedUsername));

            if (user.locked()) {
                throw new BusinessException("ACCOUNT_LOCKED", "Your account has been locked by administrator");
            }

            if (command.getUserId() != null && !command.getUserId().equals(user.userId())) {
                throw new BusinessException("IDOR_VIOLATION", "User ID does not match authenticated user");
            }

            // 1. Idempotent Lookup: If clientRequestId provided and already committed, replay authoritative booking
            java.util.Optional<Booking> existingOpt = bookingRepository.findByUserIdAndClientRequestId(user.userId(), clientRequestId);
            if (existingOpt.isPresent()) {
                Booking existing = existingOpt.get();
                validateCanonicalPayloadEquality(existing, command.getTripId(), normalizedSeats);
                log.info("[HOLD IDEMPOTENCY] Replaying existing booking [{}] for user [{}] and clientRequestId [{}]",
                        existing.getBookingReference(), user.userId(), clientRequestId);
                return BookingMapper.toResponse(existing);
            }
        }

        String bookingReference = UUID.randomUUID().toString();
        LocalDateTime holdStart = LocalDateTime.now();

        // 2. Acquire Owner-Token distributed locks OUTSIDE DB transaction to avoid MySQL REPEATABLE READ stale snapshot & Hikari pool exhaustion
        boolean locked = seatLockRepository.acquireLocks(
                command.getTripId(),
                normalizedSeats,
                bookingReference,
                HOLD_SEAT_DURATION
        );

        if (!locked) {
            // If lock rejected, verify if a concurrent request with same clientRequestId already succeeded and committed
            if (clientRequestId != null && user != null) {
                java.util.Optional<Booking> existingOpt = bookingRepository.findByUserIdAndClientRequestId(user.userId(), clientRequestId);
                if (existingOpt.isPresent()) {
                    Booking existing = existingOpt.get();
                    validateCanonicalPayloadEquality(existing, command.getTripId(), normalizedSeats);
                    return BookingMapper.toResponse(existing);
                }
            }
            throw new BusinessException("SEAT_ALREADY_RESERVED", "One or more selected seats are currently held or reserved");
        }

        final IdentityUserPort.AuthenticatedUserInfo preFetchedUser = user;
        // 3. Execute short DB transaction; if unique constraint violation occurs on clientRequestId race, replay committed winner
        try {
            Booking saved = transactionTemplate.execute(status ->
                    createHeldBookingInTransaction(command, authenticatedUsername, preFetchedUser, normalizedSeats, bookingReference, holdStart, clientRequestId)
            );
            return BookingMapper.toResponse(saved);
        } catch (RuntimeException ex) {
            releaseOwnedLocksSafely(command.getTripId(), normalizedSeats, bookingReference);

            // Handle race condition: concurrent thread with same clientRequestId committed first
            if (clientRequestId != null && isUniqueConstraintViolation(ex)) {
                Long uid = preFetchedUser != null ? preFetchedUser.userId() : null;
                if (uid != null) {
                    java.util.Optional<Booking> winningOpt = bookingRepository.findByUserIdAndClientRequestId(uid, clientRequestId);
                    if (winningOpt.isPresent()) {
                        Booking winningBooking = winningOpt.get();
                        validateCanonicalPayloadEquality(winningBooking, command.getTripId(), normalizedSeats);
                        log.info("[HOLD IDEMPOTENCY] Resolved concurrent race for user [{}] and clientRequestId [{}], replaying winner [{}]",
                                uid, clientRequestId, winningBooking.getBookingReference());
                        return BookingMapper.toResponse(winningBooking);
                    }
                }
            }
            throw ex;
        }
    }

    private void validateCanonicalPayloadEquality(Booking booking, Long expectedTripId, List<String> expectedNormalizedSeats) {
        if (!booking.getTripId().equals(expectedTripId)) {
            throw new BusinessException("IDEMPOTENCY_KEY_PAYLOAD_MISMATCH",
                    "Idempotency key has already been used for trip " + booking.getTripId() + ", but requested trip is " + expectedTripId);
        }
        List<String> existingSeats = booking.getSeatNumbers().stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList();
        if (!existingSeats.equals(expectedNormalizedSeats)) {
            throw new BusinessException("IDEMPOTENCY_KEY_PAYLOAD_MISMATCH",
                    "Idempotency key has already been used with seats " + existingSeats + ", but requested seats are " + expectedNormalizedSeats);
        }
    }

    private boolean isUniqueConstraintViolation(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            String msg = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
            if (cause instanceof org.springframework.dao.DataIntegrityViolationException
                    || cause instanceof java.sql.SQLIntegrityConstraintViolationException
                    || msg.contains("duplicate entry")
                    || msg.contains("uk_booking_user_client_request")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private Booking createHeldBookingInTransaction(HoldSeatCommand command,
                                                   String authenticatedUsername,
                                                   IdentityUserPort.AuthenticatedUserInfo preFetchedUser,
                                                   List<String> normalizedSeats,
                                                   String bookingReference,
                                                   LocalDateTime holdStart,
                                                   String clientRequestId) {
        IdentityUserPort.AuthenticatedUserInfo user = preFetchedUser != null
                ? preFetchedUser
                : identityUserPort.findByUsername(authenticatedUsername)
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

        // BE-CON-001: Khóa bi quan (SELECT ... FOR UPDATE ORDER BY seat_number ASC) trên các dòng Seat vật lý trong MySQL
        // Kiểm tra số lượng dòng ghế thực tế khóa được phải khớp tuyệt đối với số ghế yêu cầu
        int lockedSeatCount = catalogTripPort.lockSeatsForBooking(command.getTripId(), normalizedSeats);
        if (lockedSeatCount != normalizedSeats.size()) {
            throw new BusinessException("SEAT_NOT_FOUND", "Could not lock all requested seats on trip: " + command.getTripId());
        }

        // findActiveBookedSeatNumbers cũng được gắn @Lock(PESSIMISTIC_WRITE) để ép MySQL REPEATABLE READ thực hiện Current Read
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
                .clientRequestId(clientRequestId)
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
        ConfirmationOutcome outcome = confirmWithOutcome(bookingId, now);
        return outcome == ConfirmationOutcome.CONFIRMED || outcome == ConfirmationOutcome.ALREADY_CONFIRMED;
    }

    @Override
    public ConfirmationOutcome confirmWithOutcome(Long bookingId, LocalDateTime now) {
        // BE-TXN-001 & BE-PAY-002: Sử dụng PROPAGATION_REQUIRED mặc định để tham gia cùng transaction với PaymentServiceImpl (VNPay IPN).
        // Sử dụng findByIdForUpdate (PESSIMISTIC_WRITE) để tuần tự hóa với Scheduler và trả về ConfirmationOutcome tường minh,
        // phân biệt rõ CONFIRMED lần đầu với ALREADY_CONFIRMED mà không ném exception nghiệp vụ ra khỏi callback.
        ConfirmationOutcome result = transactionTemplate.execute(status -> {
            Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                    .orElseThrow(() -> new EntityNotFoundException("Booking", bookingId));
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                log.info("Booking {} is ALREADY_CONFIRMED when confirming payment", booking.getBookingReference());
                return ConfirmationOutcome.ALREADY_CONFIRMED;
            }
            if (booking.getStatus() != BookingStatus.HELD) {
                log.warn("Cannot confirm booking {} because status is {}", bookingId, booking.getStatus());
                return ConfirmationOutcome.EXPIRED_OR_INVALID;
            }
            if (booking.isExpired(now)) {
                log.warn("Cannot confirm booking {} because hold time expired at {}", bookingId, booking.getExpiresAt());
                return ConfirmationOutcome.EXPIRED_OR_INVALID;
            }
            booking.confirm(now);
            bookingRepository.save(booking);
            log.info("Booking {} confirmed successfully", booking.getBookingReference());
            return ConfirmationOutcome.CONFIRMED;
        });
        return result != null ? result : ConfirmationOutcome.EXPIRED_OR_INVALID;
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
                Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
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
