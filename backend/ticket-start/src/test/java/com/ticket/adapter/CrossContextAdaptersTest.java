package com.ticket.adapter;

import com.ticket.booking.domain.port.CatalogTripPort;
import com.ticket.booking.domain.port.IdentityUserPort;
import com.ticket.catalog.domain.model.Seat;
import com.ticket.catalog.domain.model.SeatType;
import com.ticket.catalog.domain.model.Trip;
import com.ticket.catalog.domain.model.TripStatus;
import com.ticket.catalog.domain.repository.TripRepository;
import com.ticket.config.GlobalExceptionHandler;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.domain.repository.PaymentRepository;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrossContextAdaptersTest {

    @Mock private TripRepository tripRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentRepository paymentRepository;

    @Test
    void catalogTripPortAdapter_shouldMapSeatPricesAndAvailability() {
        CatalogTripPortAdapter adapter = new CatalogTripPortAdapter(tripRepository);
        Trip trip = Trip.builder()
                .id(1L)
                .status(TripStatus.SCHEDULED)
                .departureTime(LocalDateTime.now().plusDays(1))
                .basePrice(BigDecimal.valueOf(1000000))
                .build();
        Seat vipSeat = Seat.builder().id(11L).seatNumber("01A").seatType(SeatType.VIP).priceMultiplier(new BigDecimal("1.50")).build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.findSeatsByTripId(1L)).thenReturn(List.of(vipSeat));

        Optional<CatalogTripPort.TripSnapshot> snapshot = adapter.findTripWithSeats(1L);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().availableForBooking()).isTrue();
        assertThat(snapshot.get().seatsByNumber().get("01A").price()).isEqualByComparingTo("1500000.00");
    }

    @Test
    void identityUserPortAdapter_shouldMapUserLockStatus() {
        IdentityUserPortAdapter adapter = new IdentityUserPortAdapter(userRepository);
        User user = User.builder().id(2L).username("customer1").status(UserStatus.LOCKED).build();
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));

        Optional<IdentityUserPort.AuthenticatedUserInfo> info = adapter.findByUsername("customer1");

        assertThat(info).isPresent();
        assertThat(info.get().userId()).isEqualTo(2L);
        assertThat(info.get().locked()).isTrue();
    }

    @Test
    void paymentRefundPortAdapter_whenPaymentExists_shouldMarkRefundedAndReturnTrue() {
        PaymentRefundPortAdapter adapter = new PaymentRefundPortAdapter(paymentRepository);
        Payment payment = Payment.builder().id(5L).bookingId(20L).status(PaymentStatus.SUCCESS).build();

        when(paymentRepository.findFirstByBookingIdAndStatusOrderByIdDesc(20L, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        boolean result = adapter.refundByBookingId(20L, BigDecimal.valueOf(500000), "Weather");

        assertThat(result).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundAmount()).isEqualByComparingTo("500000");
        verify(paymentRepository).save(payment);
    }

    @Test
    void paymentRefundPortAdapter_whenNoPaymentExists_shouldReturnFalseGracefully() {
        PaymentRefundPortAdapter adapter = new PaymentRefundPortAdapter(paymentRepository);
        when(paymentRepository.findFirstByBookingIdAndStatusOrderByIdDesc(20L, PaymentStatus.SUCCESS))
                .thenReturn(Optional.empty());

        boolean result = adapter.refundByBookingId(20L, BigDecimal.valueOf(500000), "Weather");

        assertThat(result).isFalse();
    }

    @Test
    void accessDeniedShouldReturn403() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.handleAccessDenied(new AccessDeniedException("Forbidden"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void generalException_shouldHideInternalDetailsAndReturn500() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.handleGeneral(new RuntimeException("Sensitive database syntax error: Table 'users' doesn't exist"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getMessage()).doesNotContain("database syntax error");
        assertThat(response.getBody().getMessage()).contains("unexpected internal server error");
    }

    @Test
    void globalExceptionHandler_shouldReturn409ForOptimisticLockingFailure() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.handleOptimisticLocking(new ObjectOptimisticLockingFailureException("BookingJpaEntity", 1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CONCURRENT_MODIFICATION");
    }

    @Test
    void paymentMarkRefunded_whenStatusNotSuccess_shouldThrowInvalidPaymentStatus() {
        Payment pendingPayment = Payment.builder().id(9L).status(PaymentStatus.PENDING).amount(BigDecimal.valueOf(1000)).build();

        assertThatThrownBy(() -> pendingPayment.markRefunded(BigDecimal.valueOf(500), "Cancel"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_PAYMENT_STATUS");
    }
}
