package com.ticket.catalog.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticket.catalog.application.dto.CreateStationCommand;
import com.ticket.catalog.application.dto.CreateTripCommand;
import com.ticket.catalog.application.dto.StationDto;
import com.ticket.catalog.application.dto.TripDto;
import com.ticket.catalog.application.usecase.CancelTripUseCase;
import com.ticket.catalog.application.usecase.CreateStationUseCase;
import com.ticket.catalog.application.usecase.CreateTripUseCase;
import com.ticket.catalog.domain.model.TransportType;
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
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminCatalogController WebMvc Unit Tests")
class AdminCatalogControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CreateStationUseCase createStationUseCase;

    @Mock
    private CreateTripUseCase createTripUseCase;

    @Mock
    private CancelTripUseCase cancelTripUseCase;

    @Mock
    private com.ticket.catalog.application.usecase.AdminManageCatalogUseCase manageCatalogUseCase;

    @InjectMocks
    private AdminCatalogController adminCatalogController;

    @BeforeEach
    void setUp() {
        adminCatalogController.setManageCatalogUseCase(manageCatalogUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(adminCatalogController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/v1/admin/stations: Should return 200 OK with StationDto")
    void createStation_WhenValid_ShouldReturnOk() throws Exception {
        CreateStationCommand command = CreateStationCommand.builder()
                .code("CXR")
                .name("Sân bay Quốc tế Cam Ranh")
                .city("Khánh Hòa")
                .build();

        StationDto responseDto = StationDto.builder()
                .id(4L)
                .code("CXR")
                .name("Sân bay Quốc tế Cam Ranh")
                .city("Khánh Hòa")
                .build();

        when(createStationUseCase.execute(any(CreateStationCommand.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/admin/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("CXR"))
                .andExpect(jsonPath("$.data.city").value("Khánh Hòa"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/trips: Should return 200 OK with created TripDto")
    void createTrip_WhenValid_ShouldReturnOk() throws Exception {
        CreateTripCommand command = CreateTripCommand.builder()
                .tripCode("VN789")
                .departureStationId(1L)
                .arrivalStationId(2L)
                .departureTime(LocalDateTime.of(2026, 12, 1, 9, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 1, 11, 15))
                .transportType(TransportType.FLIGHT)
                .basePrice(new BigDecimal("1500000.00"))
                .totalRows(10)
                .seatsPerRow(6)
                .vipRows(2)
                .build();

        TripDto responseDto = TripDto.builder()
                .id(10L)
                .tripCode("VN789")
                .departureStationCode("HAN")
                .departureStationName("Sân bay Nội Bài")
                .arrivalStationCode("SGN")
                .arrivalStationName("Sân bay Tân Sơn Nhất")
                .transportType("FLIGHT")
                .basePrice(new BigDecimal("1500000.00"))
                .totalSeats(60)
                .status("SCHEDULED")
                .build();

        when(createTripUseCase.execute(any(CreateTripCommand.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/admin/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripCode").value("VN789"))
                .andExpect(jsonPath("$.data.totalSeats").value(60))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/trips/{id}/cancel: Should return 200 OK with CANCELLED status")
    void cancelTrip_ShouldReturnOk() throws Exception {
        TripDto responseDto = TripDto.builder()
                .id(10L)
                .tripCode("VN789")
                .status("CANCELLED")
                .build();

        when(cancelTripUseCase.execute(10L)).thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/admin/trips/10/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET, PUT, DELETE /api/v1/admin/stations & /trips: Should support full catalog CRUD")
    void fullCatalogCrud_ShouldReturnOk() throws Exception {
        StationDto stationDto = StationDto.builder().id(1L).code("HAN").name("Nội Bài").city("Hà Nội").build();
        when(manageCatalogUseCase.getAllStations()).thenReturn(java.util.List.of(stationDto));
        when(manageCatalogUseCase.updateStation(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(stationDto);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/admin/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("HAN"));

        CreateStationCommand updateStationCmd = CreateStationCommand.builder().code("HAN").name("Nội Bài T2").city("Hà Nội").build();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/admin/stations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStationCmd)))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/admin/stations/1"))
                .andExpect(status().isOk());

        TripDto tripDto = TripDto.builder().id(10L).tripCode("VN789").status("SCHEDULED").build();
        when(manageCatalogUseCase.getAllTrips()).thenReturn(java.util.List.of(tripDto));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/admin/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tripCode").value("VN789"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/admin/trips/10"))
                .andExpect(status().isOk());
    }
}
