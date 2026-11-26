package com.ticket.booking.infrastructure.service;

import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.SeatAvailabilityResponse;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingItem;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.port.CatalogTripPort;
import com.ticket.booking.domain.port.IdentityUserPort;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.booking.domain.repository.SeatLockRepository;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingQueryServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private IdentityUserPort identityUserPort;
    @Mock private CatalogTripPort catalogTripPort;
    @Mock private SeatLockRepository seatLockRepository;

    @InjectMocks
    private BookingQueryServiceImpl queryService;

    @Test
    void getMyBookings_shouldClampOversizedPageSizeTo100() {
        when(identityUserPort.findByUsername("customer1"))
                .thenReturn(Optional.of(new IdentityUserPort.AuthenticatedUserInfo(2L, false)));
        when(bookingRepository.findByUserId(2L, 0, 100)).thenReturn(PageResponse.<Booking>builder()
                .items(List.of())
                .page(0)
                .size(100)
                .totalElements(0)
                .totalPages(0)
                .build());

        PageResponse<BookingResponse> result = queryService.execute("customer1", -1, 5000);

        assertThat(result.getSize()).isEqualTo(100);
        verify(bookingRepository).findByUserId(2L, 0, 100);
    }

    @Test
    void getBookingDetail_whenOwnedByAnotherUser_shouldThrowAccessDenied() {
        when(identityUserPort.findByUsername("customer1"))
                .thenReturn(Optional.of(new IdentityUserPort.AuthenticatedUserInfo(2L, false)));
        when(bookingRepository.findByReference("REF-99")).thenReturn(Optional.of(Booking.builder()
                .id(99L)
                .bookingReference("REF-99")
                .userId(77L)
                .items(List.of(BookingItem.builder().seatId(1L).seatNumber("01A").price(BigDecimal.TEN).build()))
                .build()));

        assertThatThrownBy(() -> queryService.execute("REF-99", "customer1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("BOOKING_ACCESS_DENIED");
    }

    @Test
    void adminGetBookings_shouldForwardFiltersAndClampPageSize() {
        when(bookingRepository.findAllWithFilters(BookingStatus.CONFIRMED, 1L, 2L, 0, 20))
                .thenReturn(PageResponse.<Booking>builder().items(List.of()).page(0).size(20).build());

        PageResponse<BookingResponse> response = queryService.execute(BookingStatus.CONFIRMED, 1L, 2L, 0, 20);

        assertThat(response.getItems()).isEmpty();
        verify(bookingRepository).findAllWithFilters(BookingStatus.CONFIRMED, 1L, 2L, 0, 20);
    }

    @Test
    void getBookingDetail_whenOwnedByCurrentUser_shouldReturnBookingWithItems() {
        when(identityUserPort.findByUsername("customer1"))
                .thenReturn(Optional.of(new IdentityUserPort.AuthenticatedUserInfo(2L, false)));
        when(bookingRepository.findByReference("REF-22")).thenReturn(Optional.of(Booking.builder()
                .id(22L)
                .bookingReference("REF-22")
                .userId(2L)
                .tripId(1L)
                .status(BookingStatus.HELD)
                .totalAmount(BigDecimal.valueOf(1500000))
                .items(List.of(BookingItem.builder().seatId(101L).seatNumber("01A").price(BigDecimal.valueOf(1500000)).build()))
                .build()));

        BookingResponse response = queryService.execute("REF-22", "customer1");

        assertThat(response.getBookingReference()).isEqualTo("REF-22");
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void getMyBookings_whenUserIsLocked_shouldThrowAccountLocked() {
        when(identityUserPort.findByUsername("customer1"))
                .thenReturn(Optional.of(new IdentityUserPort.AuthenticatedUserInfo(2L, true)));

        assertThatThrownBy(() -> queryService.execute("customer1", 0, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("ACCOUNT_LOCKED");
    }

    @Test
    void getSeatAvailability_whenTripNotFound_shouldThrowEntityNotFoundException() {
        when(catalogTripPort.findTripWithSeats(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.execute(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getSeatAvailability_whenTripHasNoSeats_shouldReturnEmptySeatsList() {
        CatalogTripPort.TripSnapshot emptySnapshot = new CatalogTripPort.TripSnapshot(101L, true, Collections.emptyMap());
        when(catalogTripPort.findTripWithSeats(101L)).thenReturn(Optional.of(emptySnapshot));

        SeatAvailabilityResponse response = queryService.execute(101L);

        assertThat(response.getTripId()).isEqualTo(101L);
        assertThat(response.getSeats()).isEmpty();
    }

    @Test
    void getSeatAvailability_shouldEnforcePrecedenceSoldOverHeldOverAvailable() {
        Map<String, CatalogTripPort.TripSeatInfo> seatMap = Map.of(
                "A01", new CatalogTripPort.TripSeatInfo(1L, "A01", BigDecimal.valueOf(350000)),
                "A02", new CatalogTripPort.TripSeatInfo(2L, "A02", BigDecimal.valueOf(350000)),
                "A03", new CatalogTripPort.TripSeatInfo(3L, "A03", BigDecimal.valueOf(350000)),
                "A04", new CatalogTripPort.TripSeatInfo(4L, "A04", BigDecimal.valueOf(350000)),
                "A05", new CatalogTripPort.TripSeatInfo(5L, "A05", BigDecimal.valueOf(350000))
        );
        CatalogTripPort.TripSnapshot snapshot = new CatalogTripPort.TripSnapshot(101L, true, seatMap);
        when(catalogTripPort.findTripWithSeats(101L)).thenReturn(Optional.of(snapshot));

        // DB Confirmed: A04, A05
        when(bookingRepository.findConfirmedSeatNumbers(101L)).thenReturn(Set.of("A04", "A05"));
        // DB Active Held: A03
        when(bookingRepository.findActiveHeldSeatNumbers(eq(101L), any(LocalDateTime.class))).thenReturn(Set.of("A03"));
        // Redis In-flight Locks: A02, A05 (A05 overlaps with confirmed DB)
        when(seatLockRepository.findLockedSeats(eq(101L), any())).thenReturn(Set.of("A02", "A05"));

        SeatAvailabilityResponse response = queryService.execute(101L);

        assertThat(response.getTripId()).isEqualTo(101L);
        assertThat(response.getSeats()).hasSize(5);

        Map<String, String> statusBySeat = response.getSeats().stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.ticket.booking.application.dto.SeatAvailabilityItemResponse::getSeatNumber,
                        com.ticket.booking.application.dto.SeatAvailabilityItemResponse::getStatus
                ));

        // A01: not in DB, not in Redis -> AVAILABLE
        assertThat(statusBySeat.get("A01")).isEqualTo("AVAILABLE");
        // A02: in Redis in-flight lock -> HELD
        assertThat(statusBySeat.get("A02")).isEqualTo("HELD");
        // A03: in DB active held -> HELD
        assertThat(statusBySeat.get("A03")).isEqualTo("HELD");
        // A04: in DB confirmed -> SOLD
        assertThat(statusBySeat.get("A04")).isEqualTo("SOLD");
        // A05: in both DB confirmed AND Redis -> SOLD (Precedence SOLD > HELD)
        assertThat(statusBySeat.get("A05")).isEqualTo("SOLD");
    }
}
