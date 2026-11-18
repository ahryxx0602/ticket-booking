package com.ticket.booking.infrastructure.service;

import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.HoldSeatCommand;
import com.ticket.booking.application.usecase.CancelBookingUseCase;
import com.ticket.booking.application.usecase.HoldSeatUseCase;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingItem;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.booking.domain.repository.SeatLockRepository;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingApplicationServiceImpl implements HoldSeatUseCase, CancelBookingUseCase {

    private final BookingRepository bookingRepository;
    private final SeatLockRepository seatLockRepository;
    private static final Duration HOLD_SEAT_DURATION = Duration.ofMinutes(10);

    @Override
    @Transactional
    public BookingResponse execute(HoldSeatCommand command) {
        // 1. Acquire distributed lock for requested seats via Redis
        boolean locked = seatLockRepository.acquireLocks(
                command.getTripId(),
                command.getSeatNumbers(),
                command.getUserId(),
                HOLD_SEAT_DURATION
        );

        if (!locked) {
            throw new BusinessException("SEAT_ALREADY_RESERVED", "One or more selected seats are currently held or reserved");
        }

        // 2. Create Booking in HELD status
        BigDecimal defaultSeatPrice = BigDecimal.valueOf(500000); // 500,000 VND sample
        BigDecimal total = defaultSeatPrice.multiply(BigDecimal.valueOf(command.getSeatNumbers().size()));

        Booking booking = Booking.builder()
                .bookingReference(UUID.randomUUID().toString())
                .userId(command.getUserId())
                .tripId(command.getTripId())
                .status(BookingStatus.HELD)
                .totalAmount(total)
                .expiresAt(LocalDateTime.now().plus(HOLD_SEAT_DURATION))
                .items(command.getSeatNumbers().stream().map(sn -> BookingItem.builder()
                        .seatNumber(sn)
                        .price(defaultSeatPrice)
                        .build()
                ).collect(Collectors.toList()))
                .build();

        Booking saved = bookingRepository.save(booking);

        return BookingResponse.builder()
                .bookingId(saved.getId())
                .bookingReference(saved.getBookingReference())
                .tripId(saved.getTripId())
                .status(saved.getStatus())
                .totalAmount(saved.getTotalAmount())
                .expiresAt(saved.getExpiresAt())
                .seatNumbers(command.getSeatNumbers())
                .build();
    }

    @Override
    @Transactional
    public void execute(String bookingReference) {
        Booking booking = bookingRepository.findByReference(bookingReference)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingReference));

        booking.transitionTo(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Release distributed locks
        List<String> seats = booking.getItems().stream().map(BookingItem::getSeatNumber).collect(Collectors.toList());
        seatLockRepository.releaseLocks(booking.getTripId(), seats);
    }
}
