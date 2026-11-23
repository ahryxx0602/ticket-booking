package com.ticket.adapter;

import com.ticket.booking.application.usecase.ConfirmBookingUseCase;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.payment.domain.port.PaymentBookingConfirmPort;
import com.ticket.payment.domain.port.PaymentBookingPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCrossContextAdaptersTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ConfirmBookingUseCase confirmBookingUseCase;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentBookingPortAdapter bookingPortAdapter;
    @InjectMocks
    private PaymentBookingConfirmPortAdapter confirmPortAdapter;
    @InjectMocks
    private PaymentIdentityPortAdapter identityPortAdapter;

    @Test
    void paymentBookingPortAdapter_whenFound_shouldReturnSnapshot() {
        Booking booking = Booking.builder()
                .id(101L)
                .bookingReference("BKG_SNAPSHOT")
                .userId(5L)
                .totalAmount(BigDecimal.valueOf(1500000))
                .status(BookingStatus.HELD)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(bookingRepository.findByReference("BKG_SNAPSHOT")).thenReturn(Optional.of(booking));

        Optional<PaymentBookingPort.BookingSnapshot> snapshot = bookingPortAdapter.findBookingByReference("BKG_SNAPSHOT");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().id()).isEqualTo(101L);
        assertThat(snapshot.get().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500000));
        assertThat(snapshot.get().isHeld()).isTrue();
    }

    @Test
    void paymentBookingConfirmPortAdapter_whenConfirmed_shouldReturnConfirmed() {
        when(confirmBookingUseCase.execute(eq(101L), any(LocalDateTime.class))).thenReturn(true);

        PaymentBookingConfirmPort.BookingConfirmResult result = confirmPortAdapter.confirmBooking(101L, LocalDateTime.now());

        assertThat(result).isEqualTo(PaymentBookingConfirmPort.BookingConfirmResult.CONFIRMED);
    }

    @Test
    void paymentBookingConfirmPortAdapter_whenExpired_shouldReturnReconciliationRequired() {
        Booking booking = Booking.builder()
                .id(101L)
                .status(BookingStatus.EXPIRED)
                .build();

        when(confirmBookingUseCase.execute(eq(101L), any(LocalDateTime.class))).thenReturn(false);
        when(bookingRepository.findById(101L)).thenReturn(Optional.of(booking));

        PaymentBookingConfirmPort.BookingConfirmResult result = confirmPortAdapter.confirmBooking(101L, LocalDateTime.now());

        assertThat(result).isEqualTo(PaymentBookingConfirmPort.BookingConfirmResult.EXPIRED_RECONCILIATION_REQUIRED);
    }

    @Test
    void paymentBookingConfirmPortAdapter_whenAlreadyConfirmed_shouldReturnAlreadyConfirmed() {
        Booking booking = Booking.builder()
                .id(102L)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(confirmBookingUseCase.execute(eq(102L), any(LocalDateTime.class))).thenReturn(false);
        when(bookingRepository.findById(102L)).thenReturn(Optional.of(booking));

        PaymentBookingConfirmPort.BookingConfirmResult result = confirmPortAdapter.confirmBooking(102L, LocalDateTime.now());

        assertThat(result).isEqualTo(PaymentBookingConfirmPort.BookingConfirmResult.ALREADY_CONFIRMED);
    }

    @Test
    void paymentBookingConfirmPortAdapter_whenExceptionThrown_shouldReturnExpiredReconciliationRequired() {
        Booking booking = Booking.builder()
                .id(103L)
                .status(BookingStatus.EXPIRED)
                .build();

        when(confirmBookingUseCase.execute(eq(103L), any(LocalDateTime.class)))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(Booking.class, 103L));
        when(bookingRepository.findById(103L)).thenReturn(Optional.of(booking));

        PaymentBookingConfirmPort.BookingConfirmResult result = confirmPortAdapter.confirmBooking(103L, LocalDateTime.now());

        assertThat(result).isEqualTo(PaymentBookingConfirmPort.BookingConfirmResult.EXPIRED_RECONCILIATION_REQUIRED);
    }

    @Test
    void paymentIdentityPortAdapter_whenActiveUser_shouldReturnId() {
        User user = User.builder()
                .id(7L)
                .username("customer_active")
                .build();

        when(userRepository.findByUsername("customer_active")).thenReturn(Optional.of(user));

        Optional<Long> userId = identityPortAdapter.findActiveUserIdByUsername("customer_active");

        assertThat(userId).isPresent().contains(7L);
    }
}
