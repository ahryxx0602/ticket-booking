package com.ticket.catalog.domain.model;

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
public class Trip extends BaseEntity {
    private Long id;
    private String tripCode;
    private Station departureStation;
    private Station arrivalStation;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private TransportType transportType;
    private Integer totalSeats;
    private BigDecimal basePrice;
    private TripStatus status;
    private List<Seat> seats;

    public void cancel() {
        this.status = TripStatus.CANCELLED;
    }

    public void markDeparted() {
        this.status = TripStatus.DEPARTED;
    }

    public boolean isAvailableForBooking() {
        return TripStatus.SCHEDULED.equals(this.status) 
                && departureTime != null 
                && departureTime.isAfter(LocalDateTime.now());
    }
}
