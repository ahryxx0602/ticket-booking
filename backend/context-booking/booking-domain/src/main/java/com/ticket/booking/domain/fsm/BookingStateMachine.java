package com.ticket.booking.domain.fsm;

import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.shared.exception.BusinessException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Finite State Machine (FSM) controlling Booking status transitions
 */
public class BookingStateMachine {

    private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS = Map.of(
            BookingStatus.PENDING, EnumSet.of(BookingStatus.HELD, BookingStatus.CANCELLED),
            BookingStatus.HELD, EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED, BookingStatus.EXPIRED),
            BookingStatus.CONFIRMED, EnumSet.of(BookingStatus.CANCELLED),
            BookingStatus.CANCELLED, EnumSet.noneOf(BookingStatus.class),
            BookingStatus.EXPIRED, EnumSet.noneOf(BookingStatus.class)
    );

    public static void validateTransition(BookingStatus current, BookingStatus target) {
        Set<BookingStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(BookingStatus.class));
        if (!allowed.contains(target)) {
            throw new BusinessException(
                    "INVALID_STATE_TRANSITION",
                    String.format("Cannot transition booking from %s to %s", current, target)
            );
        }
    }
}
