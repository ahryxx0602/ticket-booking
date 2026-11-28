package com.ticket.partner.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartnerCatalogPort {
    boolean checkVehicleScheduleOverlap(Long vehicleId, LocalDateTime start, LocalDateTime end, Long excludeTripId);
    Optional<Long> findOperatorIdByTripId(Long tripId);
    boolean isTripSettled(Long tripId);
    List<UnsettledTripDto> findUnsettledTripsBefore(LocalDateTime threshold);
    void markTripSettled(Long tripId);
    boolean markTripSettledIfUnsettled(Long tripId);
    BigDecimal getTotalTicketSalesByTripId(Long tripId);
    Long createPartnerTrip(Long operatorId, Long vehicleId, String tripCode, Long departureStationId, Long arrivalStationId, LocalDateTime departureTime, LocalDateTime arrivalTime, BigDecimal basePrice, int totalSeats);
    com.ticket.shared.dto.PageResponse<PartnerTripDto> findOperatorTrips(Long operatorId, String status, Long vehicleId, int page, int size);
}
