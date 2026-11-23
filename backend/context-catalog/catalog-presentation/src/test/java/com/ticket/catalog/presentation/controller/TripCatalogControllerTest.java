package com.ticket.catalog.presentation.controller;

import com.ticket.catalog.application.dto.SeatDto;
import com.ticket.catalog.application.dto.StationDto;
import com.ticket.catalog.application.dto.TripDto;
import com.ticket.catalog.application.usecase.GetStationsUseCase;
import com.ticket.catalog.application.usecase.GetTripSeatsUseCase;
import com.ticket.catalog.application.usecase.SearchTripsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripCatalogController Public WebMvc Unit Tests")
class TripCatalogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SearchTripsUseCase searchTripsUseCase;

    @Mock
    private GetStationsUseCase getStationsUseCase;

    @Mock
    private GetTripSeatsUseCase getTripSeatsUseCase;

    @InjectMocks
    private TripCatalogController tripCatalogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tripCatalogController).build();
    }

    @Test
    @DisplayName("GET /api/v1/catalog/trips: Should return 200 OK with scheduled trips")
    void searchTrips_ShouldReturnOk() throws Exception {
        TripDto tripDto = TripDto.builder()
                .id(1L)
                .tripCode("VN789")
                .departureStationCode("HAN")
                .arrivalStationCode("SGN")
                .status("SCHEDULED")
                .build();

        when(searchTripsUseCase.execute(any())).thenReturn(List.of(tripDto));

        mockMvc.perform(get("/api/v1/catalog/trips")
                        .param("departureCity", "Hà Nội")
                        .param("arrivalCity", "Hồ Chí Minh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].tripCode").value("VN789"));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/stations: Should return 200 OK with station list")
    void getStations_ShouldReturnOk() throws Exception {
        StationDto station = StationDto.builder()
                .id(1L)
                .code("HAN")
                .name("Sân bay Nội Bài")
                .city("Hà Nội")
                .build();

        when(getStationsUseCase.execute()).thenReturn(List.of(station));

        mockMvc.perform(get("/api/v1/catalog/stations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("HAN"));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/trips/{id}/seats: Should return 200 OK with SeatDto matrix")
    void getTripSeats_ShouldReturnOk() throws Exception {
        SeatDto vipSeat = SeatDto.builder()
                .id(101L)
                .tripId(1L)
                .seatNumber("01A")
                .seatType("VIP")
                .priceMultiplier(new BigDecimal("1.50"))
                .finalPrice(new BigDecimal("2250000.00"))
                .build();

        when(getTripSeatsUseCase.execute(1L)).thenReturn(List.of(vipSeat));

        mockMvc.perform(get("/api/v1/catalog/trips/1/seats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].seatNumber").value("01A"))
                .andExpect(jsonPath("$.data[0].seatType").value("VIP"))
                .andExpect(jsonPath("$.data[0].finalPrice").value(2250000.00));
    }
}
