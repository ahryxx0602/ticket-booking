package com.ticket.partner.domain.model;

import com.ticket.shared.domain.BaseEntity;
import com.ticket.shared.exception.BusinessException;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle extends BaseEntity {
    private Long id;
    private Long operatorId;
    private String plateNumber;
    private String modelName;
    private VehicleType vehicleType;
    private Integer totalSeats;
    @Builder.Default
    private Integer seatRows = 10;
    @Builder.Default
    private Integer seatsPerRow = 4;
    @Builder.Default
    private Integer vipRows = 2;
    private VehicleStatus status;

    public void validate() {
        if (operatorId == null) {
            throw new BusinessException("INVALID_VEHICLE", "Operator ID is required");
        }
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            throw new BusinessException("INVALID_PLATE_NUMBER", "Plate number cannot be blank");
        }
        if (totalSeats == null || totalSeats <= 0 || totalSeats > 100) {
            throw new BusinessException("INVALID_SEAT_COUNT", "Total seats must be between 1 and 100");
        }
    }

    public void setMaintenance() {
        this.status = VehicleStatus.MAINTENANCE;
    }

    public void activate() {
        this.status = VehicleStatus.ACTIVE;
    }
}
