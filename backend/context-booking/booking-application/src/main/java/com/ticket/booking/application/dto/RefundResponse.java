package com.ticket.booking.application.dto;

import com.ticket.booking.domain.model.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private String bookingReference;
    private BookingStatus status;
    private Integer refundPercentage;
    private BigDecimal refundAmount;
    private boolean paymentRefunded;
    private String reason;
    private LocalDateTime refundedAt;
}
