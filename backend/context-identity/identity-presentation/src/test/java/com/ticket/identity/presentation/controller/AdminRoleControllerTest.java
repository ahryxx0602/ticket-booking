package com.ticket.identity.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.identity.application.dto.PermissionModuleCatalogDto;
import com.ticket.identity.application.dto.SystemRoleRequest;
import com.ticket.identity.application.dto.SystemRoleResponse;
import com.ticket.identity.application.usecase.AdminRoleManagementUseCase;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminRoleController WebMvc Unit Tests")
class AdminRoleControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminRoleManagementUseCase roleManagementUseCase;

    @InjectMocks
    private AdminRoleController adminRoleController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminRoleController).build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/roles/permissions-catalog & GET /api/v1/admin/roles: Should return catalog and roles")
    void getCatalogAndRoles_ShouldReturnOk() throws Exception {
        PermissionModuleCatalogDto mod = PermissionModuleCatalogDto.builder()
                .moduleCode("MOD_DASHBOARD")
                .moduleName("1. Tổng quan & Đối soát")
                .routePath("/admin")
                .viewPermissionCode("PAYMENT_REPORT_VIEW")
                .permissions(List.of())
                .build();
        when(roleManagementUseCase.getPermissionCatalog()).thenReturn(List.of(mod));

        mockMvc.perform(get("/api/v1/admin/roles/permissions-catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].moduleCode").value("MOD_DASHBOARD"));

        SystemRoleResponse roleResp = SystemRoleResponse.builder()
                .id(4L)
                .code("ROLE_ACCOUNTANT")
                .name("Kế Toán & Đối Soát")
                .category("SYSTEM")
                .permissions(List.of("PAYMENT_REPORT_VIEW"))
                .systemDefault(false)
                .userCount(1L)
                .build();
        when(roleManagementUseCase.getAllRoles()).thenReturn(List.of(roleResp));

        mockMvc.perform(get("/api/v1/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("ROLE_ACCOUNTANT"));
    }

    @Test
    @DisplayName("POST, PUT, DELETE /api/v1/admin/roles: Should create, update and delete custom role")
    void createUpdateDeleteRole_ShouldReturnSuccess() throws Exception {
        SystemRoleRequest req = SystemRoleRequest.builder()
                .code("ROLE_CONTENT_MOD")
                .name("Kiểm Duyệt Nội Dung")
                .description("Kiểm duyệt banner")
                .permissions(List.of("BANNER_VIEW", "BANNER_APPROVE_REJECT"))
                .build();

        SystemRoleResponse resp = SystemRoleResponse.builder()
                .id(8L)
                .code("ROLE_CONTENT_MOD")
                .name("Kiểm Duyệt Nội Dung")
                .permissions(List.of("BANNER_VIEW", "BANNER_APPROVE_REJECT"))
                .build();

        when(roleManagementUseCase.createRole(any(SystemRoleRequest.class), anyString())).thenReturn(resp);
        when(roleManagementUseCase.updateRole(eq(8L), any(SystemRoleRequest.class), anyString())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/admin/roles")
                        .principal(() -> "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("ROLE_CONTENT_MOD"));

        mockMvc.perform(put("/api/v1/admin/roles/8")
                        .principal(() -> "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(delete("/api/v1/admin/roles/8")
                        .principal(() -> "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(roleManagementUseCase).deleteRole(8L, "admin");
    }
}
