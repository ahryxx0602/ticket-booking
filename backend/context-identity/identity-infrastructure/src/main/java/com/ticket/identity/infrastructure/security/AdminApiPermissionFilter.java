package com.ticket.identity.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bộ lọc cưỡng chế phân quyền động theo ma trận 7 Modules x 26 APIs cho các endpoints /api/v1/admin/**.
 * Chạy SAU AuthorizationFilter để giữ nguyên 401 cho Guest và 403 ACCESS_DENIED cho Customer/Operator.
 */
public class AdminApiPermissionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/v1/admin")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (!authorities.contains("ROLE_ADMIN")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isCustomAdminRole = authorities.stream().anyMatch(a -> a.startsWith("CUSTOM_ROLE_"));

        // PC-01 & PC-04: Nếu là Super Admin (ROLE_ADMIN gốc không mang CUSTOM_ROLE_*), cho phép toàn quyền
        if (!isCustomAdminRole) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
        List<String> allowedPerms = resolveRequiredPermissions(method, uri);

        // FND-005: Fail-Closed! Nếu endpoint /api/v1/admin/** không khớp bất kỳ rule nào hoặc user thiếu quyền -> Chặn 403
        boolean granted = !allowedPerms.isEmpty() && allowedPerms.stream()
                .anyMatch(reqPerm -> authorities.contains("PERM_" + reqPerm));

        if (!granted) {
            String missingCode = allowedPerms.isEmpty() ? "UNMAPPED_ADMIN_ENDPOINT" : allowedPerms.get(0);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"success\":false,\"code\":\"FORBIDDEN\",\"message\":\"Bạn không có quyền thực hiện thao tác API này (Thiếu quyền: "
                            + missingCode + ")\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * PA-03, PC-06 & BE-SEC-003: Thuật toán Specificity-First + Exact AntPattern/HttpMethod Matching.
     * Mọi tổ hợp (HttpMethod, URI) không khớp chính xác với 26 API quản trị chuẩn sẽ trả về List.of() -> Fail-Closed 403!
     */
    private static final org.springframework.util.AntPathMatcher PATH_MATCHER = new org.springframework.util.AntPathMatcher();

    private record EndpointRule(Set<String> methods, String pattern, List<String> permissions) {}

    private static final List<EndpointRule> ENDPOINT_RULES = List.of(
            // 1. MODULE 1: DASHBOARD & PAYMENTS
            new EndpointRule(Set.of("GET"), "/api/v1/admin/payments/reports", List.of("PAYMENT_REPORT_VIEW")),
            new EndpointRule(Set.of("POST"), "/api/v1/admin/payments/*/reconcile", List.of("PAYMENT_RECONCILE_RESOLVE")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/payments", List.of("PAYMENT_LIST_VIEW")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/payments/*", List.of("PAYMENT_LIST_VIEW")),

            // 2. GLOBAL COMMISSION & SYSTEM SETTINGS
            new EndpointRule(Set.of("GET", "PUT"), "/api/v1/admin/partners/commission/global", List.of("SYSTEM_SETTINGS_MANAGE")),
            new EndpointRule(Set.of("GET", "PUT"), "/api/v1/admin/settings", List.of("SYSTEM_SETTINGS_MANAGE")),

            // 3. MODULE 2: OPERATORS
            new EndpointRule(Set.of("PUT"), "/api/v1/admin/partners/operators/*/approve", List.of("OPERATOR_APPROVE_REJECT")),
            new EndpointRule(Set.of("PUT"), "/api/v1/admin/partners/operators/*/reject", List.of("OPERATOR_APPROVE_REJECT")),
            new EndpointRule(Set.of("PUT"), "/api/v1/admin/partners/operators/*/suspend", List.of("OPERATOR_SUSPEND")),
            new EndpointRule(Set.of("PUT"), "/api/v1/admin/partners/operators/*/commission", List.of("OPERATOR_COMMISSION_MANAGE")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/partners/operators", List.of("OPERATOR_VIEW")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/partners/operators/*", List.of("OPERATOR_VIEW")),
            new EndpointRule(Set.of("POST"), "/api/v1/admin/partners/operators", List.of("OPERATOR_CREATE_UPDATE")),
            new EndpointRule(Set.of("PUT", "DELETE"), "/api/v1/admin/partners/operators/*", List.of("OPERATOR_CREATE_UPDATE")),

            // 4. MODULE 3: WITHDRAWALS
            new EndpointRule(Set.of("PUT"), "/api/v1/admin/partners/withdrawals/*/approve", List.of("WITHDRAWAL_APPROVE")),
            new EndpointRule(Set.of("PUT"), "/api/v1/admin/partners/withdrawals/*/reject", List.of("WITHDRAWAL_REJECT")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/partners/withdrawals", List.of("WITHDRAWAL_VIEW")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/partners/withdrawals/*", List.of("WITHDRAWAL_VIEW")),

            // 5. MODULE 4: BANNERS
            new EndpointRule(Set.of("PUT"), "/api/v1/admin/partners/banners/*/approve", List.of("BANNER_APPROVE_REJECT")),
            new EndpointRule(Set.of("PUT"), "/api/v1/admin/partners/banners/*/reject", List.of("BANNER_APPROVE_REJECT")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/partners/banners", List.of("BANNER_VIEW")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/partners/banners/*", List.of("BANNER_VIEW")),
            new EndpointRule(Set.of("POST"), "/api/v1/admin/partners/banners", List.of("BANNER_CREATE_DELETE")),
            new EndpointRule(Set.of("POST"), "/api/v1/admin/partners/banners/platform", List.of("BANNER_CREATE_DELETE")),
            new EndpointRule(Set.of("PUT", "DELETE"), "/api/v1/admin/partners/banners/*", List.of("BANNER_CREATE_DELETE")),

            // 6. MODULE 5: IAM - USERS
            new EndpointRule(Set.of("PATCH"), "/api/v1/admin/users/*/status", List.of("USER_STATUS_LOCK")),
            new EndpointRule(Set.of("POST"), "/api/v1/admin/users/*/reset-password", List.of("USER_PASSWORD_RESET")),
            new EndpointRule(Set.of("PATCH"), "/api/v1/admin/users/*/role", List.of("USER_ROLE_ASSIGN")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/users", List.of("USER_VIEW")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/users/*", List.of("USER_VIEW")),
            new EndpointRule(Set.of("POST"), "/api/v1/admin/users", List.of("USER_CREATE_UPDATE_DELETE")),
            new EndpointRule(Set.of("PUT", "DELETE"), "/api/v1/admin/users/*", List.of("USER_CREATE_UPDATE_DELETE")),

            // 7. MODULE 5: IAM - ROLES
            new EndpointRule(Set.of("GET"), "/api/v1/admin/roles/permissions-catalog", List.of("ROLE_MANAGE_CRUD", "USER_VIEW", "USER_CREATE_UPDATE_DELETE", "USER_ROLE_ASSIGN")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/roles", List.of("ROLE_MANAGE_CRUD", "USER_VIEW", "USER_CREATE_UPDATE_DELETE", "USER_ROLE_ASSIGN")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/roles/*", List.of("ROLE_MANAGE_CRUD", "USER_VIEW", "USER_CREATE_UPDATE_DELETE", "USER_ROLE_ASSIGN")),
            new EndpointRule(Set.of("POST"), "/api/v1/admin/roles", List.of("ROLE_MANAGE_CRUD")),
            new EndpointRule(Set.of("PUT", "DELETE"), "/api/v1/admin/roles/*", List.of("ROLE_MANAGE_CRUD")),

            // 8. MODULE 6: BOOKINGS
            new EndpointRule(Set.of("POST"), "/api/v1/admin/bookings/*/refund", List.of("BOOKING_FORCE_REFUND")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/bookings", List.of("BOOKING_VIEW")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/bookings/*", List.of("BOOKING_VIEW")),

            // 9. MODULE 7: STATIONS & TRIPS
            new EndpointRule(Set.of("GET"), "/api/v1/admin/stations", List.of("STATION_VIEW", "TRIP_MANAGE_CRUD")),
            new EndpointRule(Set.of("GET"), "/api/v1/admin/stations/*", List.of("STATION_VIEW", "TRIP_MANAGE_CRUD")),
            new EndpointRule(Set.of("POST"), "/api/v1/admin/stations", List.of("STATION_CREATE_UPDATE_DELETE")),
            new EndpointRule(Set.of("PUT", "DELETE"), "/api/v1/admin/stations/*", List.of("STATION_CREATE_UPDATE_DELETE")),
            new EndpointRule(Set.of("GET", "POST"), "/api/v1/admin/trips", List.of("TRIP_MANAGE_CRUD")),
            new EndpointRule(Set.of("GET", "PUT", "PATCH", "DELETE"), "/api/v1/admin/trips/*", List.of("TRIP_MANAGE_CRUD")),
            new EndpointRule(Set.of("PUT", "PATCH", "POST"), "/api/v1/admin/trips/*/cancel", List.of("TRIP_MANAGE_CRUD"))
    );

    public static List<String> resolveRequiredPermissions(String method, String uri) {
        if (method == null || uri == null) {
            return List.of();
        }
        String cleanMethod = method.trim().toUpperCase();
        String path = uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;

        for (EndpointRule rule : ENDPOINT_RULES) {
            if (rule.methods().contains(cleanMethod) && PATH_MATCHER.match(rule.pattern(), path)) {
                return rule.permissions();
            }
        }

        return List.of();
    }
}
