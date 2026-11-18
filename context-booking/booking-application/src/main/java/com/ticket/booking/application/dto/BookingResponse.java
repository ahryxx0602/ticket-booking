package com.ticket.booking.application.dto;

import com.ticket.booking.domain.model.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long bookingId;
    private String bookingReference;
    private Long tripId;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime expiresAt;
    private List<String> seatNumbers;
}
