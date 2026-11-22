package com.ticket.booking.domain.model;

import com.ticket.booking.domain.fsm.BookingStateMachine;
import com.ticket.shared.domain.BaseEntity;
import com.ticket.shared.exception.BusinessException;
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
    private Long version;
    private List<BookingItem> items;

    public void transitionTo(BookingStatus newStatus) {
        BookingStateMachine.validateTransition(this.status, newStatus);
        this.status = newStatus;
    }

    public void ensureOwnedBy(Long currentUserId) {
        if (currentUserId == null || !currentUserId.equals(this.userId)) {
            throw new BusinessException("BOOKING_ACCESS_DENIED", "You do not have permission to access this booking");
        }
    }

    public boolean isExpired(LocalDateTime now) {
        return this.expiresAt != null && !this.expiresAt.isAfter(now);
    }

    public void cancelByCustomer(Long currentUserId, LocalDateTime now) {
        ensureOwnedBy(currentUserId);
        if (this.status != BookingStatus.HELD) {
            throw new BusinessException("INVALID_BOOKING_STATUS", "Only HELD bookings can be cancelled by customer");
        }
        if (isExpired(now)) {
            throw new BusinessException("BOOKING_EXPIRED", "Booking hold period has already expired");
        }
        transitionTo(BookingStatus.CANCELLED);
    }

    public void confirm(LocalDateTime now) {
        if (this.status == BookingStatus.CONFIRMED) {
            return;
        }
        if (this.status != BookingStatus.HELD) {
            throw new BusinessException("INVALID_BOOKING_STATUS", "Only HELD bookings can be confirmed: " + this.status);
        }
        if (isExpired(now)) {
            throw new BusinessException("BOOKING_EXPIRED", "Booking hold period has expired at " + this.expiresAt);
        }
        transitionTo(BookingStatus.CONFIRMED);
    }

    public void expire(LocalDateTime now) {
        if (this.status == BookingStatus.EXPIRED) {
            return;
        }
        if (this.status != BookingStatus.HELD) {
            throw new BusinessException("INVALID_BOOKING_STATUS", "Only HELD bookings can be expired: " + this.status);
        }
        transitionTo(BookingStatus.EXPIRED);
    }

    public void refundByAdmin() {
        if (this.status != BookingStatus.CONFIRMED) {
            throw new BusinessException("INVALID_BOOKING_STATUS", "Only CONFIRMED bookings can be refunded by admin");
        }
        transitionTo(BookingStatus.CANCELLED);
    }
}
