package com.ticket.partner.application.dto;

import com.ticket.partner.domain.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCommand {
    private String plateNumber;
    private String modelName;
    private VehicleType vehicleType;
    private Integer totalSeats;
    private Integer seatRows;
    private Integer seatsPerRow;
    private Integer vipRows;
}
