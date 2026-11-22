package com.ticket.catalog.infrastructure.service;

import com.ticket.catalog.application.dto.*;
import com.ticket.catalog.domain.model.*;
import com.ticket.catalog.domain.repository.StationRepository;
import com.ticket.catalog.domain.repository.TripRepository;
import com.ticket.catalog.infrastructure.cache.TripRedisCacheService;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminCatalogServiceImpl & SearchTripsUseCaseImpl Unit Tests")
class AdminCatalogServiceImplTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripRedisCacheService cacheService;

    @InjectMocks
    private AdminCatalogServiceImpl adminCatalogService;

    @InjectMocks
    private SearchTripsUseCaseImpl publicCatalogService;

    private Station hanStation;
    private Station sgnStation;

    @BeforeEach
    void setUp() {
        hanStation = Station.builder().id(1L).code("HAN").name("Sân bay Nội Bài").city("Hà Nội").build();
        sgnStation = Station.builder().id(2L).code("SGN").name("Sân bay Tân Sơn Nhất").city("Hồ Chí Minh").build();
    }

    @Test
    @DisplayName("createStation: Should normalize code to uppercase and save when unique")
    void createStation_WhenUnique_ShouldSaveAndReturnDto() {
        CreateStationCommand command = CreateStationCommand.builder()
                .code(" cxr ")
                .name(" Sân bay Cam Ranh ")
                .city(" Khánh Hòa ")
                .build();

        when(stationRepository.existsByCode("CXR")).thenReturn(false);
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> {
            Station s = inv.getArgument(0);
            s.setId(4L);
            return s;
        });

        StationDto result = adminCatalogService.execute(command);

        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getCode()).isEqualTo("CXR");
        assertThat(result.getName()).isEqualTo("Sân bay Cam Ranh");
        assertThat(result.getCity()).isEqualTo("Khánh Hòa");
    }

    @Test
    @DisplayName("createStation: Should throw STATION_CODE_EXISTS when code already exists")
    void createStation_WhenDuplicateCode_ShouldThrowException() {
        CreateStationCommand command = CreateStationCommand.builder()
                .code("han")
                .name("Sân bay Nội Bài")
                .city("Hà Nội")
                .build();

        when(stationRepository.existsByCode("HAN")).thenReturn(true);

        assertThatThrownBy(() -> adminCatalogService.execute(command))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("STATION_CODE_EXISTS");
    }

    @Test
    @DisplayName("getStations: Should return all stations mapped to StationDto")
    void getStations_ShouldReturnList() {
        when(stationRepository.findAll()).thenReturn(List.of(hanStation, sgnStation));

        List<StationDto> result = publicCatalogService.execute();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("HAN");
        assertThat(result.get(1).getCode()).isEqualTo("SGN");
    }

    @Test
    @DisplayName("createTrip: Should generate seat matrix, save trip, and evict search caches")
    void createTrip_WhenValid_ShouldGenerateSeatsSaveAndEvictCache() {
        CreateTripCommand command = CreateTripCommand.builder()
                .tripCode(" vn789 ")
                .departureStationId(1L)
                .arrivalStationId(2L)
                .departureTime(LocalDateTime.now().plusDays(2))
                .arrivalTime(LocalDateTime.now().plusDays(2).plusHours(2))
                .transportType(TransportType.FLIGHT)
                .basePrice(new BigDecimal("1500000.00"))
                .totalRows(10)
                .seatsPerRow(6)
                .vipRows(2)
                .build();

        when(tripRepository.existsByTripCode("VN789")).thenReturn(false);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(hanStation));
        when(stationRepository.findById(2L)).thenReturn(Optional.of(sgnStation));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        TripDto result = adminCatalogService.execute(command);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTripCode()).isEqualTo("VN789");
        assertThat(result.getTotalSeats()).isEqualTo(60);
        assertThat(result.getStatus()).isEqualTo("SCHEDULED");
        assertThat(result.getDepartureStationCode()).isEqualTo("HAN");
        assertThat(result.getArrivalStationCode()).isEqualTo("SGN");
        verify(cacheService).evictAllSearchCaches();
    }

    @Test
    @DisplayName("createTrip: Should throw TRIP_CODE_EXISTS when tripCode already exists")
    void createTrip_WhenDuplicateTripCode_ShouldThrowException() {
        CreateTripCommand command = CreateTripCommand.builder()
                .tripCode("VN789")
                .departureStationId(1L)
                .arrivalStationId(2L)
                .build();

        when(tripRepository.existsByTripCode("VN789")).thenReturn(true);

        assertThatThrownBy(() -> adminCatalogService.execute(command))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("TRIP_CODE_EXISTS");
    }

    @Test
    @DisplayName("cancelTrip: Should transition SCHEDULED trip to CANCELLED and evict cache")
    void cancelTrip_WhenScheduled_ShouldCancelAndEvictCache() {
        Trip scheduledTrip = Trip.builder()
                .id(10L)
                .tripCode("VN789")
                .departureStation(hanStation)
                .arrivalStation(sgnStation)
                .departureTime(LocalDateTime.now().plusDays(1))
                .arrivalTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .transportType(TransportType.FLIGHT)
                .basePrice(new BigDecimal("1500000.00"))
                .totalSeats(60)
                .status(TripStatus.SCHEDULED)
                .build();

        when(tripRepository.findById(10L)).thenReturn(Optional.of(scheduledTrip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        TripDto result = adminCatalogService.execute(10L);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(cacheService).evictAllSearchCaches();
    }

    @Test
    @DisplayName("getTripSeats: Should throw EntityNotFoundException when trip does not exist")
    void getTripSeats_WhenTripNotFound_ShouldThrow404() {
        when(tripRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicCatalogService.execute(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Trip with id '999' not found");
    }

    @Test
    @DisplayName("getTripSeats: Should calculate finalPrice with scale 2 for VIP and STANDARD seats")
    void getTripSeats_WhenTripExists_ShouldReturnSeatsWithFinalPrice() {
        Trip trip = Trip.builder()
                .id(10L)
                .tripCode("VN789")
                .basePrice(new BigDecimal("1500000.00"))
                .status(TripStatus.SCHEDULED)
                .build();

        Seat vipSeat = Seat.builder().id(101L).tripId(10L).seatNumber("01A").seatType(SeatType.VIP).priceMultiplier(new BigDecimal("1.50")).build();
        Seat stdSeat = Seat.builder().id(102L).tripId(10L).seatNumber("03A").seatType(SeatType.STANDARD).priceMultiplier(new BigDecimal("1.00")).build();

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripRepository.findSeatsByTripId(10L)).thenReturn(List.of(vipSeat, stdSeat));

        List<SeatDto> seats = publicCatalogService.execute(10L);

        assertThat(seats).hasSize(2);
        assertThat(seats.get(0).getSeatNumber()).isEqualTo("01A");
        assertThat(seats.get(0).getFinalPrice()).isEqualTo(new BigDecimal("2250000.00"));
        assertThat(seats.get(1).getSeatNumber()).isEqualTo("03A");
        assertThat(seats.get(1).getFinalPrice()).isEqualTo(new BigDecimal("1500000.00"));
    }
}
