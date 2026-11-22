package com.ticket.booking.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.booking.application.dto.AdminRefundCommand;
import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.RefundResponse;
import com.ticket.booking.application.usecase.AdminGetBookingsUseCase;
import com.ticket.booking.application.usecase.AdminRefundBookingUseCase;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminBookingControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private AdminGetBookingsUseCase adminGetBookingsUseCase;
    @Mock private AdminRefundBookingUseCase adminRefundBookingUseCase;

    @InjectMocks
    private AdminBookingController adminBookingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminBookingController).build();
    }

    @Test
    void getBookings_shouldReturnFilteredPageResponse() throws Exception {
        when(adminGetBookingsUseCase.execute(BookingStatus.CONFIRMED, 1L, 2L, 0, 10))
                .thenReturn(PageResponse.<BookingResponse>builder().items(List.of()).page(0).size(10).build());

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .param("status", "CONFIRMED")
                        .param("tripId", "1")
                        .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void refundBooking_whenValid_shouldReturnRefundResponse() throws Exception {
        AdminRefundCommand command = AdminRefundCommand.builder()
                .reason("Flight cancelled due to weather")
                .refundPercentage(100)
                .build();

        RefundResponse response = RefundResponse.builder()
                .bookingReference("REF-20")
                .status(BookingStatus.CANCELLED)
                .refundPercentage(100)
                .refundAmount(BigDecimal.valueOf(1500000))
                .paymentRefunded(true)
                .reason("Flight cancelled due to weather")
                .build();

        when(adminRefundBookingUseCase.execute(eq("REF-20"), any(AdminRefundCommand.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/bookings/REF-20/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.paymentRefunded").value(true));
    }

    @Test
    void refundBooking_whenPercentageOutOfRange_shouldReturnBadRequest() throws Exception {
        AdminRefundCommand command = AdminRefundCommand.builder()
                .reason("Invalid percentage")
                .refundPercentage(150)
                .build();

        mockMvc.perform(post("/api/v1/admin/bookings/REF-20/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest());
    }
}
