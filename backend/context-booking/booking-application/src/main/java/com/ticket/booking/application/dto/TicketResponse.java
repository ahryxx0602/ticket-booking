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
public class TicketResponse {
    private String bookingReference;
    private Long tripId;
    private List<String> seatNumbers;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private String qrPayload;
    private String qrImageBase64;
    private LocalDateTime issuedAt;
}
