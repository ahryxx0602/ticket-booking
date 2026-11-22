package com.ticket.catalog.domain.model;

import com.ticket.shared.domain.BaseEntity;
import com.ticket.shared.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public void validateScheduleAndRoute() {
        if (departureStation == null || arrivalStation == null
                || departureStation.getId() == null || arrivalStation.getId() == null
                || departureStation.getId().equals(arrivalStation.getId())) {
            throw new BusinessException("INVALID_TRIP_ROUTE", "Departure and arrival stations cannot be the same");
        }
        if (departureTime == null || arrivalTime == null || !arrivalTime.isAfter(departureTime)) {
            throw new BusinessException("INVALID_TRIP_TIME", "Arrival time must be after departure time");
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_BASE_PRICE", "Base price must be greater than zero");
        }
    }

    public void generateSeatMatrix(int totalRows, int seatsPerRow, int vipRows) {
        if (totalRows < 1 || totalRows > 60 || seatsPerRow < 1 || seatsPerRow > 10 || vipRows < 0 || vipRows > totalRows) {
            throw new BusinessException("INVALID_SEAT_MATRIX", "Invalid seat matrix dimensions: rows (1..60), seatsPerRow (1..10), vipRows (0..totalRows)");
        }

        List<Seat> generatedSeats = new ArrayList<>(totalRows * seatsPerRow);
        for (int row = 1; row <= totalRows; row++) {
            SeatType type = (row <= vipRows) ? SeatType.VIP : SeatType.STANDARD;
            BigDecimal multiplier = type.getDefaultMultiplier();
            for (int col = 0; col < seatsPerRow; col++) {
                char colLetter = (char) ('A' + col);
                String seatNumber = String.format("%02d%c", row, colLetter);
                generatedSeats.add(Seat.builder()
                        .tripId(this.id)
                        .seatNumber(seatNumber)
                        .seatType(type)
                        .priceMultiplier(multiplier)
                        .build());
            }
        }
        this.seats = generatedSeats;
        this.totalSeats = generatedSeats.size();
    }

    public void cancel() {
        if (this.status != TripStatus.SCHEDULED) {
            throw new BusinessException("INVALID_TRIP_STATUS", "Only SCHEDULED trips can be cancelled");
        }
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
