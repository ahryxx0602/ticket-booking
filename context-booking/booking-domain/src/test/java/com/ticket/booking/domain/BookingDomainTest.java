package com.ticket.booking.domain;

import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingDomainTest {

    @Test
    void transitionPendingToHeld_shouldSucceed() {
        Booking booking = Booking.builder()
                .bookingReference("REF-1")
                .userId(10L)
                .tripId(1L)
                .status(BookingStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(1500000))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        booking.transitionTo(BookingStatus.HELD);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    void cancelByCustomer_whenOwnerAndHeldAndNotExpired_shouldTransitionToCancelled() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = Booking.builder()
                .bookingReference("REF-2")
                .userId(10L)
                .status(BookingStatus.HELD)
                .expiresAt(now.plusMinutes(5))
                .build();

        booking.cancelByCustomer(10L, now);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancelByCustomer_whenDifferentUser_shouldThrowAccessDenied() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = Booking.builder()
                .bookingReference("REF-3")
                .userId(10L)
                .status(BookingStatus.HELD)
                .expiresAt(now.plusMinutes(5))
                .build();

        assertThatThrownBy(() -> booking.cancelByCustomer(99L, now))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("BOOKING_ACCESS_DENIED");
    }

    @Test
    void cancelByCustomer_whenExpired_shouldThrowBookingExpired() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = Booking.builder()
                .bookingReference("REF-4")
                .userId(10L)
                .status(BookingStatus.HELD)
                .expiresAt(now.minusSeconds(1))
                .build();

        assertThatThrownBy(() -> booking.cancelByCustomer(10L, now))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("BOOKING_EXPIRED");
    }

    @Test
    void cancelByCustomer_whenConfirmed_shouldThrowInvalidBookingStatus() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = Booking.builder()
                .bookingReference("REF-5")
                .userId(10L)
                .status(BookingStatus.CONFIRMED)
                .expiresAt(now.plusMinutes(5))
                .build();

        assertThatThrownBy(() -> booking.cancelByCustomer(10L, now))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_BOOKING_STATUS");
    }

    @Test
    void refundByAdmin_whenConfirmed_shouldTransitionToCancelled() {
        Booking booking = Booking.builder()
                .bookingReference("REF-6")
                .userId(10L)
                .status(BookingStatus.CONFIRMED)
                .build();

        booking.refundByAdmin();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void refundByAdmin_whenHeld_shouldThrowInvalidBookingStatus() {
        Booking booking = Booking.builder()
                .bookingReference("REF-7")
                .userId(10L)
                .status(BookingStatus.HELD)
                .build();

        assertThatThrownBy(booking::refundByAdmin)
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_BOOKING_STATUS");
    }
}
