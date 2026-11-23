package com.ticket.partner.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverCommand {
    private String fullName;
    private String phone;
    private String licenseNumber;
}
