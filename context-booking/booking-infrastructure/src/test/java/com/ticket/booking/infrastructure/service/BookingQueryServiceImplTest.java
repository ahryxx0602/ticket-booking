package com.ticket.booking.infrastructure.service;

import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingItem;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.port.IdentityUserPort;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingQueryServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private IdentityUserPort identityUserPort;

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
}
