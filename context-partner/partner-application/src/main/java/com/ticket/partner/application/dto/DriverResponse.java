package com.ticket.partner.application.dto;

import com.ticket.partner.domain.model.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponse {
    private Long id;
    private Long operatorId;
    private String fullName;
    private String phone;
    private String licenseNumber;
    private DriverStatus status;
}
