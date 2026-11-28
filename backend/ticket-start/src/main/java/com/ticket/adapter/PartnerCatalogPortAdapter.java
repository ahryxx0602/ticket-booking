package com.ticket.adapter;

import com.ticket.booking.infrastructure.persistence.repository.SpringDataBookingRepository;
import com.ticket.catalog.infrastructure.persistence.entity.TripJpaEntity;
import com.ticket.catalog.infrastructure.persistence.repository.SpringDataTripRepository;
import com.ticket.partner.domain.port.PartnerCatalogPort;
import com.ticket.partner.domain.port.UnsettledTripDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PartnerCatalogPortAdapter implements PartnerCatalogPort {

    private final SpringDataTripRepository tripRepository;
    private final SpringDataBookingRepository bookingRepository;

    @Override
    public boolean checkVehicleScheduleOverlap(Long vehicleId, LocalDateTime start, LocalDateTime end, Long excludeTripId) {
        if (vehicleId == null || start == null || end == null) {
            return false;
        }
        return tripRepository.checkVehicleScheduleOverlap(vehicleId, start, end, excludeTripId);
    }

    @Override
    public Optional<Long> findOperatorIdByTripId(Long tripId) {
        if (tripId == null) return Optional.empty();
        return tripRepository.findById(tripId).map(TripJpaEntity::getOperatorId);
    }

    @Override
    public boolean isTripSettled(Long tripId) {
        if (tripId == null) return false;
        return tripRepository.findById(tripId)
                .map(t -> "SETTLED".equalsIgnoreCase(t.getSettlementStatus()))
                .orElse(false);
    }

    @Override
    public List<UnsettledTripDto> findUnsettledTripsBefore(LocalDateTime threshold) {
        return tripRepository.findUnsettledTripsBefore(threshold).stream()
                .map(t -> new UnsettledTripDto(t.getId(), t.getOperatorId(), t.getDepartureTime()))
                .toList();
    }

    @Override
    @Transactional
    public void markTripSettled(Long tripId) {
        tripRepository.findById(tripId).ifPresent(t -> {
            t.setSettlementStatus("SETTLED");
            tripRepository.save(t);
        });
    }

    @Override
    @Transactional
    public boolean markTripSettledIfUnsettled(Long tripId) {
        if (tripId == null) return false;
        return tripRepository.markTripSettledIfUnsettled(tripId) > 0;
    }

    @Override
    public BigDecimal getTotalTicketSalesByTripId(Long tripId) {
        if (tripId == null) return BigDecimal.ZERO;
        return bookingRepository.sumConfirmedAmountByTripId(tripId);
    }

    @Override
    @Transactional
    public Long createPartnerTrip(Long operatorId, Long vehicleId, String tripCode, Long departureStationId, Long arrivalStationId, LocalDateTime departureTime, LocalDateTime arrivalTime, BigDecimal basePrice, int totalSeats) {
        TripJpaEntity entity = new TripJpaEntity();
        entity.setTripCode(tripCode);
        entity.setOperatorId(operatorId);
        entity.setVehicleId(vehicleId);
        var depStation = new com.ticket.catalog.infrastructure.persistence.entity.StationJpaEntity();
        depStation.setId(departureStationId);
        entity.setDepartureStation(depStation);
        var arrStation = new com.ticket.catalog.infrastructure.persistence.entity.StationJpaEntity();
        arrStation.setId(arrivalStationId);
        entity.setArrivalStation(arrStation);
        entity.setDepartureTime(departureTime);
        entity.setArrivalTime(arrivalTime);
        entity.setTransportType(com.ticket.catalog.domain.model.TransportType.BUS);
        entity.setTotalSeats(totalSeats);
        entity.setBasePrice(basePrice);
        entity.setStatus(com.ticket.catalog.domain.model.TripStatus.SCHEDULED);
        entity.setSettlementStatus("UNSETTLED");
        TripJpaEntity saved = tripRepository.save(entity);
        return saved.getId();
    }

    @Override
    public com.ticket.shared.dto.PageResponse<com.ticket.partner.domain.port.PartnerTripDto> findOperatorTrips(Long operatorId, String status, Long vehicleId, int page, int size) {
        if (operatorId == null) {
            return com.ticket.shared.dto.PageResponse.of(java.util.Collections.emptyList(), page, size, 0);
        }
        com.ticket.catalog.domain.model.TripStatus tripStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                tripStatus = com.ticket.catalog.domain.model.TripStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        int validPage = Math.max(0, page);
        int validSize = size <= 0 ? 10 : Math.min(size, 100);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(validPage, validSize);

        org.springframework.data.domain.Page<TripJpaEntity> resultPage = tripRepository.findByOperatorIdWithFilters(operatorId, tripStatus, vehicleId, pageable);

        List<com.ticket.partner.domain.port.PartnerTripDto> dtoList = resultPage.getContent().stream()
                .map(t -> com.ticket.partner.domain.port.PartnerTripDto.builder()
                        .id(t.getId())
                        .tripCode(t.getTripCode())
                        .operatorId(t.getOperatorId())
                        .vehicleId(t.getVehicleId())
                        .departureStationId(t.getDepartureStation() != null ? t.getDepartureStation().getId() : null)
                        .arrivalStationId(t.getArrivalStation() != null ? t.getArrivalStation().getId() : null)
                        .departureTime(t.getDepartureTime())
                        .arrivalTime(t.getArrivalTime())
                        .basePrice(t.getBasePrice())
                        .status(t.getStatus() != null ? t.getStatus().name() : "SCHEDULED")
                        .build())
                .toList();

        return com.ticket.shared.dto.PageResponse.of(dtoList, validPage, validSize, resultPage.getTotalElements());
    }
}
