package com.ticket.partner.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOperatorProfileCommand {
    private String brandName;
    private String contactPhone;
    private String contactEmail;
    private String logoUrl;
    private String description;
}
