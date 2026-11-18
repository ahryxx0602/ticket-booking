package com.ticket.booking.domain.model;

import com.ticket.booking.domain.fsm.BookingStateMachine;
import com.ticket.shared.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends BaseEntity {
    private Long id;
    private String bookingReference;
    private Long userId;
    private Long tripId;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime expiresAt;
    private List<BookingItem> items;

    public void transitionTo(BookingStatus newStatus) {
        BookingStateMachine.validateTransition(this.status, newStatus);
        this.status = newStatus;
    }
}
