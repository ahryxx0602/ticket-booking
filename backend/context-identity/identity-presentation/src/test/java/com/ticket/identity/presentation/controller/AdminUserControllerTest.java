package com.ticket.identity.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.identity.application.dto.AdminResetPasswordRequest;
import com.ticket.identity.application.dto.ChangeUserRoleRequest;
import com.ticket.identity.application.dto.UpdateUserStatusRequest;
import com.ticket.identity.application.dto.UserDetailResponse;
import com.ticket.identity.application.usecase.*;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.shared.dto.PageResponse;
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

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserController Unit & WebMvc Tests")
class AdminUserControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AdminGetUsersUseCase getUsersUseCase;

    @Mock
    private AdminGetUserDetailUseCase getUserDetailUseCase;

    @Mock
    private AdminUpdateUserStatusUseCase updateUserStatusUseCase;

    @Mock
    private AdminChangeUserRoleUseCase changeUserRoleUseCase;

    @Mock
    private AdminResetPasswordUseCase resetPasswordUseCase;

    @InjectMocks
    private AdminUserController adminUserController;

    private UserDetailResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController).build();

        sampleResponse = UserDetailResponse.builder()
                .id(1L)
                .username("customer1")
                .email("customer1@example.com")
                .fullName("Nguyen Van Customer")
                .role("ROLE_CUSTOMER")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/users: Should return 200 OK with PageResponse")
    void getUsers_ShouldReturnOk() throws Exception {
        PageResponse<UserDetailResponse> pageResponse = PageResponse.of(List.of(sampleResponse), 0, 10, 1L);
        when(getUsersUseCase.execute(any(), any(), any(), eq(0), eq(10))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].username").value("customer1"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users/{id}: Should return 200 OK with User details")
    void getUserDetail_ShouldReturnOk() throws Exception {
        when(getUserDetailUseCase.execute(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("customer1@example.com"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/status: Should return 200 OK after lock/unlock")
    void updateUserStatus_ShouldReturnOk() throws Exception {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.LOCKED);

        sampleResponse.setStatus(UserStatus.LOCKED);
        when(updateUserStatusUseCase.execute(eq(1L), any(UpdateUserStatusRequest.class), anyString()))
                .thenReturn(sampleResponse);

        Principal principal = () -> "admin";

        mockMvc.perform(patch("/api/v1/admin/users/1/status")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/role: Should return 200 OK after changing role")
    void changeUserRole_ShouldReturnOk() throws Exception {
        ChangeUserRoleRequest request = new ChangeUserRoleRequest();
        request.setRole("ROLE_ADMIN");

        sampleResponse.setRole("ROLE_ADMIN");
        when(changeUserRoleUseCase.execute(eq(1L), any(ChangeUserRoleRequest.class), anyString()))
                .thenReturn(sampleResponse);

        Principal principal = () -> "admin";

        mockMvc.perform(patch("/api/v1/admin/users/1/role")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/users/{id}/reset-password: Should return 200 OK")
    void resetPassword_ShouldReturnOk() throws Exception {
        AdminResetPasswordRequest request = new AdminResetPasswordRequest();
        request.setNewPassword("sample_new_password");

        mockMvc.perform(post("/api/v1/admin/users/1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(resetPasswordUseCase).execute(eq(1L), any(AdminResetPasswordRequest.class));
    }
}
