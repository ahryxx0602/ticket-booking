package com.ticket.catalog.application.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SearchTripQuery {
    private String departureCity;
    private String arrivalCity;
    private LocalDate departureDate;
}
