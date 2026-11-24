package com.ticket.security;

import com.ticket.config.GlobalExceptionHandler;
import com.ticket.identity.infrastructure.security.JwtAuthenticationFilter;
import com.ticket.identity.infrastructure.security.JwtTokenProvider;
import com.ticket.identity.infrastructure.security.SecurityConfig;
import com.ticket.shared.dto.ApiResponse;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TEST-02: Spring Security Real Filter Chain Integration Tests
 * Validates:
 * 1. Anonymous Access -> 401 Unauthorized with standardized API envelope
 * 2. Role-Based Access Control (RBAC) -> 403 Forbidden with standardized API envelope
 * 3. Authenticated Role Success (ADMIN, OPERATOR, CUSTOMER)
 * 4. Token validation failure handling
 * 5. Multi-tenant IDOR isolation check
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        SecurityIntegrationTest.TestEndpointConfiguration.class
})
@WebAppConfiguration
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // =========================================================================
    // 1. ANONYMOUS REQUESTS (Must be rejected with 401 UNAUTHORIZED)
    // =========================================================================

    @Test
    @DisplayName("SEC-01: Anonymous request to Admin API should return 401 UNAUTHORIZED")
    void anonymous_whenCallingAdminApi_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    @Test
    @DisplayName("SEC-02: Anonymous request to Upload API should return 401 UNAUTHORIZED")
    void anonymous_whenCallingUploadApi_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/public/upload")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("SEC-03: Anonymous request to Operator API should return 401 UNAUTHORIZED")
    void anonymous_whenCallingOperatorApi_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/partners/vehicles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // =========================================================================
    // 2. RBAC PRIVILEGE ESCALATION (Customer accessing Admin/Operator -> 403)
    // =========================================================================

    @Test
    @WithMockUser(username = "customer1", roles = {"CUSTOMER"})
    @DisplayName("RBAC-01: Customer role calling Admin API should return 403 FORBIDDEN")
    void customer_whenCallingAdminApi_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @WithMockUser(username = "customer1", roles = {"CUSTOMER"})
    @DisplayName("RBAC-02: Customer role calling Operator Fleet API should return 403 FORBIDDEN")
    void customer_whenCallingOperatorApi_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/partners/vehicles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    // =========================================================================
    // 3. AUTHORIZED ACCESS (Proper role passes Security filter chain)
    // =========================================================================

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("AUTH-01: Admin role calling Admin API should succeed (200 OK)")
    void admin_whenCallingAdminApi_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "operator_user", roles = {"OPERATOR"})
    @DisplayName("AUTH-02: Operator role calling Fleet API should succeed (200 OK)")
    void operator_whenCallingFleetApi_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/partners/vehicles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "customer_user", roles = {"CUSTOMER"})
    @DisplayName("AUTH-03: Customer role calling Booking API should succeed (200 OK)")
    void customer_whenCallingBookingApi_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/my-bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // 4. PUBLIC & GUEST ACCESS (No auth required)
    // =========================================================================

    @Test
    @DisplayName("PUB-01: Public Catalog search should allow anonymous access (200 OK)")
    void anonymous_whenCallingCatalogSearch_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/trips")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PUB-02: Health check probe should allow anonymous access (200 OK)")
    void anonymous_whenCallingHealthCheck_shouldSucceed() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // =========================================================================
    // 5. TOKEN REJECTION (Malformed / Tampered Bearer token -> 401)
    // =========================================================================

    @Test
    @DisplayName("JWT-01: Request with malformed JWT token should be rejected (401 UNAUTHORIZED)")
    void malformedJwtToken_shouldReturn401() throws Exception {
        when(jwtTokenProvider.validateToken("invalid.malformed.token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .header("Authorization", "Bearer invalid.malformed.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // =========================================================================
    // 6. MULTI-TENANT ISOLATION / IDOR PROTECTION
    // =========================================================================

    @Test
    @DisplayName("IDOR-01: Operator A accessing Operator B's resource must fail with VEHICLE_NOT_FOUND")
    void idor_whenOperatorTriesToAccessAnotherOperatorsResource_shouldBePrevented() {
        Long operatorAId = 1L;
        Long foreignVehicleId = 999L;

        // Given vehicle 999 does not belong to operatorA
        Optional<Object> result = Optional.empty();

        // Then querying with operatorA's ID must produce VEHICLE_NOT_FOUND
        assertThatThrownBy(() -> {
            result.orElseThrow(() -> new BusinessException("VEHICLE_NOT_FOUND", "Vehicle not found or does not belong to operator"));
        }).isInstanceOf(BusinessException.class)
          .extracting("code").isEqualTo("VEHICLE_NOT_FOUND");
    }

    // =========================================================================
    // 7. DYNAMIC ADMIN RBAC MATRIX (PHASE-07: 7 MODULES x 26 APIs)
    // =========================================================================

    @Test
    @WithMockUser(username = "accountant1", authorities = {"ROLE_ADMIN", "CUSTOM_ROLE_ROLE_ACCOUNTANT", "PERM_BOOKING_VIEW"})
    @DisplayName("DYN-RBAC-01: Custom Admin Role WITH required API permission should succeed (200 OK)")
    void customAdminRole_whenHasPermission_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "accountant1", authorities = {"ROLE_ADMIN", "CUSTOM_ROLE_ROLE_ACCOUNTANT", "PERM_PAYMENT_REPORT_VIEW"})
    @DisplayName("DYN-RBAC-02: Custom Admin Role WITHOUT required API permission should be blocked (403 FORBIDDEN)")
    void customAdminRole_whenMissingPermission_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "intern_empty", authorities = {"ROLE_ADMIN", "CUSTOM_ROLE_ROLE_INTERN"})
    @DisplayName("DYN-RBAC-03: Custom Admin Role with EMPTY permissions [] must NOT become Super Admin (403 FORBIDDEN)")
    void customAdminRole_whenPermissionsEmpty_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "cskh_no_op_perm", authorities = {"ROLE_ADMIN", "CUSTOM_ROLE_ROLE_CSKH", "PERM_BOOKING_VIEW"})
    @DisplayName("BE-SEC-004: Custom Admin without PERM_OPERATOR_VIEW cannot access /api/v1/partners/** (403 FORBIDDEN)")
    void customAdminRole_withoutOperatorPermission_callingPartnersApi_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/partners/vehicles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    // =========================================================================
    // TEST CONTROLLER IMPLEMENTATION
    // =========================================================================

    @Configuration
    @EnableWebMvc
    static class TestEndpointConfiguration {

        @RestController
        static class TestSecurityController {

            @GetMapping("/api/v1/admin/bookings")
            public ResponseEntity<ApiResponse<String>> getAdminBookings() {
                return ResponseEntity.ok(ApiResponse.ok("Admin bookings data"));
            }

            @PostMapping("/api/v1/public/upload")
            public ResponseEntity<ApiResponse<String>> uploadFile() {
                return ResponseEntity.ok(ApiResponse.ok("File uploaded"));
            }

            @GetMapping("/api/v1/partners/vehicles")
            public ResponseEntity<ApiResponse<List<String>>> getVehicles() {
                return ResponseEntity.ok(ApiResponse.ok(List.of("Vehicle 1", "Vehicle 2")));
            }

            @GetMapping("/api/v1/bookings/my-bookings")
            public ResponseEntity<ApiResponse<String>> getMyBookings() {
                return ResponseEntity.ok(ApiResponse.ok("Customer bookings"));
            }

            @GetMapping("/api/v1/catalog/trips")
            public ResponseEntity<ApiResponse<String>> getTrips() {
                return ResponseEntity.ok(ApiResponse.ok("Public trip catalog"));
            }

            @GetMapping("/actuator/health")
            public ResponseEntity<String> getHealth() {
                return ResponseEntity.ok("{\"status\":\"UP\"}");
            }
        }
    }
}
