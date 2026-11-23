package com.ticket.catalog.application.usecase;

import com.ticket.catalog.application.dto.SearchTripQuery;
import com.ticket.catalog.application.dto.TripDto;
import java.util.List;

public interface SearchTripsUseCase {
    List<TripDto> execute(SearchTripQuery query);
}
