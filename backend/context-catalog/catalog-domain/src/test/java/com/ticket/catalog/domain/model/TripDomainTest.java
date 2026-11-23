package com.ticket.catalog.domain.model;

import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Trip & Seat Rich Domain Model Unit Tests")
class TripDomainTest {

    private Trip createValidSampleTrip() {
        return Trip.builder()
                .id(1L)
                .tripCode("VN2026")
                .departureStation(Station.builder().id(1L).code("HAN").name("Noi Bai").city("Ha Noi").build())
                .arrivalStation(Station.builder().id(2L).code("SGN").name("Tan Son Nhat").city("Ho Chi Minh").build())
                .departureTime(LocalDateTime.now().plusDays(1))
                .arrivalTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .transportType(TransportType.FLIGHT)
                .basePrice(new BigDecimal("1500000.00"))
                .status(TripStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("generateSeatMatrix: Should generate exact R*C seats with VIP and STANDARD tiers")
    void generateSeatMatrix_ShouldGenerateCorrectSeatsAndMultipliers() {
        Trip trip = createValidSampleTrip();
        trip.generateSeatMatrix(10, 6, 2);

        assertThat(trip.getTotalSeats()).isEqualTo(60);
        assertThat(trip.getSeats()).hasSize(60);

        Seat firstVipSeat = trip.getSeats().get(0);
        assertThat(firstVipSeat.getSeatNumber()).isEqualTo("01A");
        assertThat(firstVipSeat.getSeatType()).isEqualTo(SeatType.VIP);
        assertThat(firstVipSeat.getPriceMultiplier()).isEqualByComparingTo("1.50");
        assertThat(firstVipSeat.calculatePrice(trip.getBasePrice())).isEqualTo(new BigDecimal("2250000.00"));

        Seat lastVipSeat = trip.getSeats().get(11);
        assertThat(lastVipSeat.getSeatNumber()).isEqualTo("02F");
        assertThat(lastVipSeat.isVip()).isTrue();

        Seat firstStandardSeat = trip.getSeats().get(12);
        assertThat(firstStandardSeat.getSeatNumber()).isEqualTo("03A");
        assertThat(firstStandardSeat.getSeatType()).isEqualTo(SeatType.STANDARD);
        assertThat(firstStandardSeat.getPriceMultiplier()).isEqualByComparingTo("1.00");
        assertThat(firstStandardSeat.calculatePrice(trip.getBasePrice())).isEqualTo(new BigDecimal("1500000.00"));

        Seat lastStandardSeat = trip.getSeats().get(59);
        assertThat(lastStandardSeat.getSeatNumber()).isEqualTo("10F");
    }

    @Test
    @DisplayName("generateSeatMatrix: Should throw INVALID_SEAT_MATRIX when vipRows > totalRows or bounds invalid")
    void generateSeatMatrix_WhenInvalidBounds_ShouldThrowException() {
        Trip trip = createValidSampleTrip();

        assertThatThrownBy(() -> trip.generateSeatMatrix(10, 6, 11))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_SEAT_MATRIX");

        assertThatThrownBy(() -> trip.generateSeatMatrix(0, 6, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_SEAT_MATRIX");

        assertThatThrownBy(() -> trip.generateSeatMatrix(10, 12, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_SEAT_MATRIX");
    }

    @Test
    @DisplayName("validateScheduleAndRoute: Should throw INVALID_TRIP_ROUTE when departure and arrival stations are identical")
    void validateScheduleAndRoute_SameStations_ShouldThrowException() {
        Trip trip = createValidSampleTrip();
        trip.setArrivalStation(trip.getDepartureStation());

        assertThatThrownBy(trip::validateScheduleAndRoute)
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_TRIP_ROUTE");
    }

    @Test
    @DisplayName("validateScheduleAndRoute: Should throw INVALID_TRIP_TIME when arrivalTime <= departureTime")
    void validateScheduleAndRoute_InvalidTime_ShouldThrowException() {
        Trip trip = createValidSampleTrip();
        trip.setArrivalTime(trip.getDepartureTime().minusMinutes(10));

        assertThatThrownBy(trip::validateScheduleAndRoute)
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_TRIP_TIME");
    }

    @Test
    @DisplayName("validateScheduleAndRoute: Should throw INVALID_BASE_PRICE when basePrice <= 0")
    void validateScheduleAndRoute_InvalidBasePrice_ShouldThrowException() {
        Trip trip = createValidSampleTrip();
        trip.setBasePrice(BigDecimal.ZERO);

        assertThatThrownBy(trip::validateScheduleAndRoute)
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_BASE_PRICE");
    }

    @Test
    @DisplayName("cancel: Should transition SCHEDULED to CANCELLED and reject already CANCELLED trip")
    void cancel_ShouldTransitionAndRejectDuplicateCancel() {
        Trip trip = createValidSampleTrip();
        trip.cancel();

        assertThat(trip.getStatus()).isEqualTo(TripStatus.CANCELLED);

        assertThatThrownBy(trip::cancel)
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_TRIP_STATUS");
    }
}
