package com.ticket.booking.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.HoldSeatCommand;
import com.ticket.booking.application.usecase.CancelBookingUseCase;
import com.ticket.booking.application.usecase.GetBookingDetailUseCase;
import com.ticket.booking.application.usecase.GetMyBookingsUseCase;
import com.ticket.booking.application.usecase.HoldSeatUseCase;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.shared.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private HoldSeatUseCase holdSeatUseCase;
    @Mock private CancelBookingUseCase cancelBookingUseCase;
    @Mock private GetMyBookingsUseCase getMyBookingsUseCase;
    @Mock private GetBookingDetailUseCase getBookingDetailUseCase;
    @Mock private com.ticket.booking.application.usecase.GetSeatAvailabilityUseCase getSeatAvailabilityUseCase;

    @InjectMocks
    private BookingController bookingController;

    private final Principal customerPrincipal = () -> "customer1";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController).build();
    }

    @Test
    void holdSeats_whenValidWithoutUserId_shouldReturnOk() throws Exception {
        HoldSeatCommand command = new HoldSeatCommand();
        command.setTripId(1L);
        command.setSeatNumbers(List.of("01A", "01B"));

        BookingResponse response = BookingResponse.builder()
                .bookingId(10L)
                .bookingReference("REF-10")
                .tripId(1L)
                .status(BookingStatus.HELD)
                .totalAmount(BigDecimal.valueOf(2500000))
                .seatNumbers(List.of("01A", "01B"))
                .build();

        when(holdSeatUseCase.execute(any(HoldSeatCommand.class), eq("customer1"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings/hold")
                        .principal(customerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingReference").value("REF-10"))
                .andExpect(jsonPath("$.data.status").value("HELD"));
    }

    @Test
    void holdSeats_whenSeatListExceedsMax10_shouldReturnBadRequest() throws Exception {
        HoldSeatCommand command = new HoldSeatCommand();
        command.setTripId(1L);
        command.setSeatNumbers(List.of("01A", "01B", "01C", "01D", "01E", "01F", "02A", "02B", "02C", "02D", "02E"));

        mockMvc.perform(post("/api/v1/bookings/hold")
                        .principal(customerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelBooking_shouldInvokeUseCaseWithAuthenticatedUsername() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/REF-10/cancel")
                        .principal(customerPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(cancelBookingUseCase).execute("REF-10", "customer1");
    }

    @Test
    void getMyBookings_shouldReturnPagedBookings() throws Exception {
        when(getMyBookingsUseCase.execute("customer1", 0, 10))
                .thenReturn(PageResponse.<BookingResponse>builder().items(List.of()).page(0).size(10).build());

        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                        .principal(customerPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    void getBookingDetail_shouldReturnBookingResponse() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .bookingId(10L)
                .bookingReference("REF-10")
                .userId(2L)
                .tripId(1L)
                .status(BookingStatus.HELD)
                .seatNumbers(List.of("01A"))
                .build();
        when(getBookingDetailUseCase.execute("REF-10", "customer1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/REF-10")
                        .principal(customerPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingReference").value("REF-10"));
    }

    @Test
    void holdSeats_whenSeatNumberInvalidPattern_shouldReturnBadRequest() throws Exception {
        HoldSeatCommand command = new HoldSeatCommand();
        command.setTripId(1L);
        command.setSeatNumbers(List.of("INVALID_SEAT_NUMBER_TOO_LONG"));

        mockMvc.perform(post("/api/v1/bookings/hold")
                        .principal(customerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSeatAvailability_shouldReturnAvailabilityResponse() throws Exception {
        com.ticket.booking.application.dto.SeatAvailabilityResponse response = com.ticket.booking.application.dto.SeatAvailabilityResponse.builder()
                .tripId(101L)
                .serverTime(java.time.OffsetDateTime.now())
                .seats(List.of(
                        new com.ticket.booking.application.dto.SeatAvailabilityItemResponse("A01", "AVAILABLE"),
                        new com.ticket.booking.application.dto.SeatAvailabilityItemResponse("A02", "HELD"),
                        new com.ticket.booking.application.dto.SeatAvailabilityItemResponse("A03", "SOLD")
                ))
                .build();

        when(getSeatAvailabilityUseCase.execute(101L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/trips/101/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripId").value(101))
                .andExpect(jsonPath("$.data.seats[0].seatNumber").value("A01"))
                .andExpect(jsonPath("$.data.seats[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.seats[1].status").value("HELD"))
                .andExpect(jsonPath("$.data.seats[2].status").value("SOLD"));
    }
}
