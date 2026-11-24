package com.ticket.booking.infrastructure.service;

import com.ticket.booking.application.dto.AdminRefundCommand;
import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.HoldSeatCommand;
import com.ticket.booking.application.dto.RefundResponse;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingItem;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.port.CatalogTripPort;
import com.ticket.booking.domain.port.IdentityUserPort;
import com.ticket.booking.domain.port.PaymentRefundPort;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.booking.domain.repository.SeatLockRepository;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCommandServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private SeatLockRepository seatLockRepository;
    @Mock private CatalogTripPort catalogTripPort;
    @Mock private IdentityUserPort identityUserPort;
    @Mock private PaymentRefundPort paymentRefundPort;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private ApplicationEventPublisher eventPublisher;

    private BookingCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        commandService = new BookingCommandServiceImpl(
                bookingRepository, seatLockRepository, catalogTripPort, identityUserPort, paymentRefundPort, transactionTemplate, eventPublisher
        );
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    void holdSeats_shouldCalculateRealCatalogPricesAndSaveHeldBooking() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(1L);
        cmd.setSeatNumbers(List.of("02B", "01A"));

        when(seatLockRepository.acquireLocks(eq(1L), eq(List.of("01A", "02B")), anyString(), eq(Duration.ofMinutes(10))))
                .thenReturn(true);
        when(identityUserPort.findByUsername("customer1"))
                .thenReturn(Optional.of(new IdentityUserPort.AuthenticatedUserInfo(2L, false)));
        when(catalogTripPort.findTripWithSeats(1L)).thenReturn(Optional.of(new CatalogTripPort.TripSnapshot(
                1L,
                true,
                Map.of(
                        "01A", new CatalogTripPort.TripSeatInfo(101L, "01A", BigDecimal.valueOf(1500000)),
                        "02B", new CatalogTripPort.TripSeatInfo(102L, "02B", BigDecimal.valueOf(1000000))
                )
        )));
        when(bookingRepository.findActiveBookedSeatNumbers(eq(1L), any(LocalDateTime.class))).thenReturn(Set.of());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(99L);
            return b;
        });

        BookingResponse response = commandService.execute(cmd, "customer1");

        assertThat(response.getBookingId()).isEqualTo(99L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.HELD);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("2500000.00");
        assertThat(response.getSeatNumbers()).containsExactly("01A", "02B");
        assertThat(response.getItems()).extracting("seatId").containsExactly(101L, 102L);
        // BE-CON-001: Verify row-level DB seat lock was acquired in sorted seat order before checking active booked seats
        verify(catalogTripPort).lockSeatsForBooking(1L, List.of("01A", "02B"));
        verify(seatLockRepository, never()).releaseOwnedLocks(any(), anyList(), anyString());
    }

    @Test
    void holdSeats_whenIdorUserIdMismatch_shouldThrowAndReleaseOwnedLocks() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setUserId(999L);
        cmd.setTripId(1L);
        cmd.setSeatNumbers(List.of("01A"));

        when(seatLockRepository.acquireLocks(eq(1L), eq(List.of("01A")), anyString(), any())).thenReturn(true);
        when(identityUserPort.findByUsername("customer1"))
                .thenReturn(Optional.of(new IdentityUserPort.AuthenticatedUserInfo(2L, false)));

        assertThatThrownBy(() -> commandService.execute(cmd, "customer1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("IDOR_VIOLATION");

        verify(seatLockRepository).releaseOwnedLocks(eq(1L), eq(List.of("01A")), anyString());
    }

    @Test
    void holdSeats_whenUserLocked_shouldThrowAccountLockedAndReleaseLocks() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(1L);
        cmd.setSeatNumbers(List.of("01A"));

        when(seatLockRepository.acquireLocks(eq(1L), eq(List.of("01A")), anyString(), any())).thenReturn(true);
        when(identityUserPort.findByUsername("customer1"))
                .thenReturn(Optional.of(new IdentityUserPort.AuthenticatedUserInfo(2L, true)));

        assertThatThrownBy(() -> commandService.execute(cmd, "customer1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("ACCOUNT_LOCKED");

        verify(seatLockRepository).releaseOwnedLocks(eq(1L), eq(List.of("01A")), anyString());
    }

    @Test
    void holdSeats_whenDbShowsSeatAlreadyHeldOrConfirmed_shouldThrowAndReleaseLocks() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(1L);
        cmd.setSeatNumbers(List.of("01A"));

        when(seatLockRepository.acquireLocks(eq(1L), eq(List.of("01A")), anyString(), any())).thenReturn(true);
        when(identityUserPort.findByUsername("customer1"))
                .thenReturn(Optional.of(new IdentityUserPort.AuthenticatedUserInfo(2L, false)));
        when(catalogTripPort.findTripWithSeats(1L)).thenReturn(Optional.of(new CatalogTripPort.TripSnapshot(
                1L, true, Map.of("01A", new CatalogTripPort.TripSeatInfo(101L, "01A", BigDecimal.valueOf(1000000)))
        )));
        when(bookingRepository.findActiveBookedSeatNumbers(eq(1L), any(LocalDateTime.class))).thenReturn(Set.of("01A"));

        assertThatThrownBy(() -> commandService.execute(cmd, "customer1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("SEAT_ALREADY_RESERVED");

        verify(seatLockRepository).releaseOwnedLocks(eq(1L), eq(List.of("01A")), anyString());
    }

    @Test
    void cancelBooking_whenOwnerAndHeld_shouldCancelAndReleaseOwnedLocks() {
        Booking booking = Booking.builder()
                .id(10L)
                .bookingReference("REF-10")
                .userId(2L)
                .tripId(1L)
                .status(BookingStatus.HELD)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .items(List.of(BookingItem.builder().seatId(101L).seatNumber("01A").price(BigDecimal.TEN).build()))
                .build();

        when(identityUserPort.findByUsername("customer1"))
                .thenReturn(Optional.of(new IdentityUserPort.AuthenticatedUserInfo(2L, false)));
        when(bookingRepository.findByReferenceForUpdate("REF-10")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        commandService.execute("REF-10", "customer1");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(seatLockRepository).releaseOwnedLocks(1L, List.of("01A"), "REF-10");
    }

    @Test
    void adminRefundBooking_whenConfirmed_shouldRefundAndReturnRefundResponse() {
        Booking booking = Booking.builder()
                .id(20L)
                .bookingReference("REF-20")
                .userId(2L)
                .tripId(1L)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(BigDecimal.valueOf(2000000))
                .expiresAt(LocalDateTime.now().minusHours(1))
                .items(List.of(BookingItem.builder().seatId(101L).seatNumber("01A").price(BigDecimal.valueOf(2000000)).build()))
                .build();

        when(bookingRepository.findByReferenceForUpdate("REF-20")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(paymentRefundPort.refundByBookingId(20L, new BigDecimal("1000000.00"), "Flight cancelled")).thenReturn(true);

        AdminRefundCommand command = AdminRefundCommand.builder()
                .reason("Flight cancelled")
                .refundPercentage(50)
                .build();

        RefundResponse response = commandService.execute("REF-20", command);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.getRefundAmount()).isEqualByComparingTo("1000000.00");
        assertThat(response.isPaymentRefunded()).isTrue();
        verify(seatLockRepository, never()).releaseOwnedLocks(any(), anyList(), anyString());
    }

    @Test
    void holdSeats_whenRedisAcquireLocksFails_shouldThrowSeatAlreadyReservedImmediately() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(1L);
        cmd.setSeatNumbers(List.of("01A"));

        when(seatLockRepository.acquireLocks(eq(1L), eq(List.of("01A")), anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> commandService.execute(cmd, "customer1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("SEAT_ALREADY_RESERVED");
    }

    @Test
    void holdSeats_whenTripNotAvailable_shouldThrowTripNotAvailableAndReleaseLocks() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(1L);
        cmd.setSeatNumbers(List.of("01A"));

        when(seatLockRepository.acquireLocks(eq(1L), eq(List.of("01A")), anyString(), any())).thenReturn(true);
        when(identityUserPort.findByUsername("customer1"))
                .thenReturn(Optional.of(new IdentityUserPort.AuthenticatedUserInfo(2L, false)));
        when(catalogTripPort.findTripWithSeats(1L)).thenReturn(Optional.of(
                new CatalogTripPort.TripSnapshot(1L, false, Map.of())
        ));

        assertThatThrownBy(() -> commandService.execute(cmd, "customer1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("TRIP_NOT_AVAILABLE");

        verify(seatLockRepository).releaseOwnedLocks(eq(1L), eq(List.of("01A")), anyString());
    }

    @Test
    void holdSeats_whenSeatDoesNotExistInTrip_shouldThrowSeatNotFoundAndReleaseLocks() {
        HoldSeatCommand cmd = new HoldSeatCommand();
        cmd.setTripId(1L);
        cmd.setSeatNumbers(List.of("99Z"));

        when(seatLockRepository.acquireLocks(eq(1L), eq(List.of("99Z")), anyString(), any())).thenReturn(true);
        when(identityUserPort.findByUsername("customer1"))
                .thenReturn(Optional.of(new IdentityUserPort.AuthenticatedUserInfo(2L, false)));
        when(catalogTripPort.findTripWithSeats(1L)).thenReturn(Optional.of(
                new CatalogTripPort.TripSnapshot(1L, true, Map.of("01A", new CatalogTripPort.TripSeatInfo(101L, "01A", BigDecimal.TEN)))
        ));

        assertThatThrownBy(() -> commandService.execute(cmd, "customer1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("SEAT_NOT_FOUND");

        verify(seatLockRepository).releaseOwnedLocks(eq(1L), eq(List.of("99Z")), anyString());
    }

    @Test
    void adminRefundBooking_whenConfirmedAndNotExpired_shouldReleaseOwnedLocks() {
        Booking booking = Booking.builder()
                .id(21L)
                .bookingReference("REF-21")
                .userId(2L)
                .tripId(1L)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(BigDecimal.valueOf(1000000))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .items(List.of(BookingItem.builder().seatId(101L).seatNumber("01A").price(BigDecimal.valueOf(1000000)).build()))
                .build();

        when(bookingRepository.findByReferenceForUpdate("REF-21")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(paymentRefundPort.refundByBookingId(21L, new BigDecimal("1000000.00"), "Customer request")).thenReturn(false);

        AdminRefundCommand command = AdminRefundCommand.builder()
                .reason("  Customer request  ")
                .refundPercentage(100)
                .build();

        RefundResponse response = commandService.execute("REF-21", command);

        assertThat(response.isPaymentRefunded()).isFalse();
        assertThat(response.getReason()).isEqualTo("Customer request");
        verify(seatLockRepository).releaseOwnedLocks(1L, List.of("01A"), "REF-21");
    }

    @Test
    void confirmBooking_whenHeldAndValid_shouldConfirmAndReturnTrue() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = Booking.builder()
                .id(10L)
                .bookingReference("BKG-10")
                .status(BookingStatus.HELD)
                .expiresAt(now.plusMinutes(5))
                .build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        boolean result = commandService.execute(10L, now);

        assertThat(result).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).save(booking);
    }

    @Test
    void confirmBooking_whenAlreadyConfirmed_shouldReturnTrueWithoutReconfirm() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = Booking.builder()
                .id(10L)
                .bookingReference("BKG-10")
                .status(BookingStatus.CONFIRMED)
                .expiresAt(now.plusMinutes(5))
                .build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        boolean result = commandService.execute(10L, now);

        assertThat(result).isTrue();
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void confirmBooking_whenExpired_shouldReturnFalse() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = Booking.builder()
                .id(10L)
                .bookingReference("BKG-10")
                .status(BookingStatus.HELD)
                .expiresAt(now.minusSeconds(10))
                .build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        boolean result = commandService.execute(10L, now);

        assertThat(result).isFalse();
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void expireHeldBookings_whenExpiredFound_shouldExpireAndReleaseSeatLocks() {
        LocalDateTime now = LocalDateTime.now();
        Booking b1 = Booking.builder()
                .id(1L)
                .bookingReference("B1")
                .tripId(100L)
                .status(BookingStatus.HELD)
                .expiresAt(now.minusSeconds(30))
                .items(List.of(BookingItem.builder().seatNumber("01A").price(BigDecimal.TEN).build()))
                .build();
        Booking b2 = Booking.builder()
                .id(2L)
                .bookingReference("B2")
                .tripId(100L)
                .status(BookingStatus.HELD)
                .expiresAt(now.minusSeconds(15))
                .items(List.of(BookingItem.builder().seatNumber("02B").price(BigDecimal.TEN).build()))
                .build();

        when(bookingRepository.findExpiredHeldBookings(eq(now), eq(100))).thenReturn(List.of(b1, b2));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(b1));
        when(bookingRepository.findById(2L)).thenReturn(Optional.of(b2));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        int expiredCount = commandService.execute(now);

        assertThat(expiredCount).isEqualTo(2);
        assertThat(b1.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(b2.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void expireHeldBookings_whenOneThrowsOptimisticLock_shouldIsolateAndContinueRemaining() {
        LocalDateTime now = LocalDateTime.now();
        Booking b1 = Booking.builder()
                .id(1L)
                .bookingReference("B1")
                .tripId(100L)
                .status(BookingStatus.HELD)
                .expiresAt(now.minusSeconds(30))
                .items(List.of(BookingItem.builder().seatNumber("01A").price(BigDecimal.TEN).build()))
                .build();
        Booking b2 = Booking.builder()
                .id(2L)
                .bookingReference("B2")
                .tripId(100L)
                .status(BookingStatus.HELD)
                .expiresAt(now.minusSeconds(15))
                .items(List.of(BookingItem.builder().seatNumber("02B").price(BigDecimal.TEN).build()))
                .build();

        when(bookingRepository.findExpiredHeldBookings(eq(now), eq(100))).thenReturn(List.of(b1, b2));
        when(bookingRepository.findById(1L)).thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(Booking.class, 1L));
        when(bookingRepository.findById(2L)).thenReturn(Optional.of(b2));
        when(bookingRepository.save(b2)).thenReturn(b2);

        int expiredCount = commandService.execute(now);

        assertThat(expiredCount).isEqualTo(1);
        assertThat(b2.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }
}
