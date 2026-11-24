package com.ticket.partner.presentation;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.application.usecase.PartnerAdminService;
import com.ticket.partner.application.usecase.PartnerQueryService;
import com.ticket.partner.domain.model.OperatorStatus;
import com.ticket.partner.domain.model.WithdrawalStatus;
import com.ticket.partner.domain.port.PartnerIdentityPort;
import com.ticket.partner.presentation.controller.AdminPartnerController;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Partner Controller Standalone WebMvc Tests")
class AdminPartnerControllerTest {

    @Mock
    private PartnerAdminService adminService;
    @Mock
    private PartnerQueryService queryService;
    @Mock
    private PartnerIdentityPort identityPort;

    @InjectMocks
    private AdminPartnerController controller;

    private MockMvc mockMvc;
    private final Principal mockPrincipal = () -> "admin";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        org.mockito.Mockito.lenient().when(identityPort.findUserIdByUsername("admin")).thenReturn(Optional.of(1L));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/partners/operators/{id}/approve - Approves operator")
    void testApproveOperator() throws Exception {
        OperatorResponse resp = OperatorResponse.builder()
                .id(1L)
                .status(OperatorStatus.ACTIVE)
                .build();
        when(adminService.approveOperator(eq(1L), eq(1L))).thenReturn(resp);

        mockMvc.perform(put("/api/v1/admin/partners/operators/1/approve").principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/partners/operators/{id}/commission - Sets custom commission rate")
    void testUpdateCustomCommission() throws Exception {
        OperatorResponse resp = OperatorResponse.builder()
                .id(1L)
                .commissionRate(BigDecimal.valueOf(7.5))
                .build();
        when(adminService.updateCommissionRate(eq(1L), any(BigDecimal.class))).thenReturn(resp);

        String json = """
                {
                    "commissionRate": 7.5
                }
                """;

        mockMvc.perform(put("/api/v1/admin/partners/operators/1/commission")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.commissionRate").value(7.5));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/partners/withdrawals/{id}/approve - Approves payout")
    void testApproveWithdrawal() throws Exception {
        WithdrawalResponse resp = WithdrawalResponse.builder()
                .id(99L)
                .status(WithdrawalStatus.APPROVED)
                .adminNote("Paid via VCB Ref 8821")
                .build();
        when(adminService.approveWithdrawal(eq(99L), eq(1L), any())).thenReturn(resp);

        String json = """
                {
                    "note": "Paid via VCB Ref 8821"
                }
                """;

        mockMvc.perform(put("/api/v1/admin/partners/withdrawals/99/approve")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST, PUT, DELETE /api/v1/admin/partners/operators, /banners & /settings - Full Partner Admin CRUD")
    void testOperatorsBannersAndSettingsCrud() throws Exception {
        OperatorResponse opResp = OperatorResponse.builder().id(5L).brandName("Sao Viet").status(OperatorStatus.ACTIVE).build();
        when(adminService.createOperatorByAdmin(any(), eq(1L))).thenReturn(opResp);
        when(adminService.updateOperatorByAdmin(eq(5L), any(), eq(1L))).thenReturn(opResp);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/admin/partners/operators")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"brandName\":\"Sao Viet\",\"taxCode\":\"0109988\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.brandName").value("Sao Viet"));

        mockMvc.perform(put("/api/v1/admin/partners/operators/5")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"brandName\":\"Sao Viet VIP\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/admin/partners/operators/5")
                        .principal(mockPrincipal))
                .andExpect(status().isOk());

        when(queryService.getAllBanners(any())).thenReturn(java.util.List.of());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/admin/partners/banners")
                        .principal(mockPrincipal))
                .andExpect(status().isOk());

        when(queryService.getAllSystemSettings()).thenReturn(java.util.Map.of("defaultCommissionRate", "10.0"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/admin/settings")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultCommissionRate").value("10.0"));

        mockMvc.perform(put("/api/v1/admin/settings")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultCommissionRate\":\"12.0\"}"))
                .andExpect(status().isOk());
    }
}
