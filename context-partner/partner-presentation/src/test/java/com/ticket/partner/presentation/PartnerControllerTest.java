package com.ticket.partner.presentation;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.application.usecase.PartnerCommandService;
import com.ticket.partner.application.usecase.PartnerQueryService;
import com.ticket.partner.domain.model.OperatorStatus;
import com.ticket.partner.domain.model.VehicleStatus;
import com.ticket.partner.domain.model.VehicleType;
import com.ticket.partner.domain.model.WithdrawalStatus;
import com.ticket.partner.domain.port.PartnerIdentityPort;
import com.ticket.partner.presentation.controller.PartnerController;
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
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Partner Controller Standalone WebMvc Tests")
class PartnerControllerTest {

    @Mock
    private PartnerCommandService commandService;
    @Mock
    private PartnerQueryService queryService;
    @Mock
    private PartnerIdentityPort identityPort;

    @InjectMocks
    private PartnerController controller;

    private MockMvc mockMvc;
    private final Principal mockPrincipal = () -> "10";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        org.mockito.Mockito.lenient().when(identityPort.findUserIdByUsername("10")).thenReturn(Optional.of(10L));
    }

    @Test
    @DisplayName("POST /api/v1/partners/register - Success")
    void testRegisterOperator() throws Exception {
        OperatorResponse resp = OperatorResponse.builder()
                .id(1L)
                .userId(10L)
                .businessName("FUTA Express")
                .brandName("Phuong Trang")
                .status(OperatorStatus.PENDING_APPROVAL)
                .build();
        when(commandService.registerOperator(eq(10L), any(RegisterOperatorCommand.class)))
                .thenReturn(resp);

        String json = """
                {
                    "businessName": "FUTA Express",
                    "brandName": "Phuong Trang",
                    "taxCode": "0301239999",
                    "contactPhone": "19006067",
                    "contactEmail": "hotro@futa.vn"
                }
                """;

        mockMvc.perform(post("/api/v1/partners/register")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));
    }

    @Test
    @DisplayName("GET /api/v1/partners/vehicles - Returns vehicles list")
    void testGetMyVehicles() throws Exception {
        VehicleResponse v = VehicleResponse.builder()
                .id(100L)
                .plateNumber("51B-123.45")
                .vehicleType(VehicleType.SLEEPER)
                .totalSeats(34)
                .status(VehicleStatus.ACTIVE)
                .build();
        when(queryService.getMyVehicles(10L)).thenReturn(List.of(v));

        mockMvc.perform(get("/api/v1/partners/vehicles").principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].plateNumber").value("51B-123.45"));
    }

    @Test
    @DisplayName("POST /api/v1/partners/wallet/withdraw - Creates withdrawal request")
    void testRequestWithdrawal() throws Exception {
        WithdrawalResponse resp = WithdrawalResponse.builder()
                .id(50L)
                .operatorId(1L)
                .amount(BigDecimal.valueOf(500000))
                .status(WithdrawalStatus.PENDING)
                .build();
        when(commandService.requestWithdrawal(eq(10L), any(WithdrawalCommand.class)))
                .thenReturn(resp);

        String json = """
                {
                    "amount": 500000,
                    "bankName": "Vietcombank",
                    "accountNumber": "0123456789",
                    "accountHolderName": "NGUYEN VAN A"
                }
                """;

        mockMvc.perform(post("/api/v1/partners/wallet/withdraw")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(500000))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
}
