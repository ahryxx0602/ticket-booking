package com.ticket.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStationCommand {
    @NotBlank(message = "Station code is required")
    @Pattern(regexp = "^[A-Za-z0-9]{3,10}$", message = "Station code must be 3-10 alphanumeric characters")
    private String code;

    @NotBlank(message = "Station name is required")
    @Size(max = 100, message = "Station name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City must not exceed 50 characters")
    private String city;
}
