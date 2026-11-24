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
     * PA-03 & PC-06: Thuật toán Specificity-First Matching ánh xạ (HttpMethod, URI) -> Required Permission Code(s).
     */
    public static List<String> resolveRequiredPermissions(String method, String uri) {
        String path = uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;

        // 1. MODULE 1: DASHBOARD & PAYMENTS (/api/v1/admin/payments**)
        if (path.startsWith("/api/v1/admin/payments")) {
            if ("GET".equals(method) && path.startsWith("/api/v1/admin/payments/reports")) {
                return List.of("PAYMENT_REPORT_VIEW");
            }
            if ("POST".equals(method) && path.endsWith("/reconcile")) {
                return List.of("PAYMENT_RECONCILE_RESOLVE");
            }
            if ("GET".equals(method)) {
                return List.of("PAYMENT_LIST_VIEW");
            }
            return List.of("PAYMENT_RECONCILE_RESOLVE");
        }

        // 2. GLOBAL COMMISSION & SYSTEM SETTINGS
        if (path.startsWith("/api/v1/admin/partners/commission/global") || path.startsWith("/api/v1/admin/settings")) {
            return List.of("SYSTEM_SETTINGS_MANAGE");
        }

        // 3. MODULE 2: OPERATORS (/api/v1/admin/partners/operators**)
        if (path.startsWith("/api/v1/admin/partners/operators")) {
            if (path.endsWith("/approve") || path.endsWith("/reject")) {
                return List.of("OPERATOR_APPROVE_REJECT");
            }
            if (path.endsWith("/suspend")) {
                return List.of("OPERATOR_SUSPEND");
            }
            if (path.endsWith("/commission")) {
                return List.of("OPERATOR_COMMISSION_MANAGE");
            }
            if ("GET".equals(method)) {
                return List.of("OPERATOR_VIEW");
            }
            return List.of("OPERATOR_CREATE_UPDATE");
        }

        // 4. MODULE 3: WITHDRAWALS (/api/v1/admin/partners/withdrawals**)
        if (path.startsWith("/api/v1/admin/partners/withdrawals")) {
            if (path.endsWith("/approve")) {
                return List.of("WITHDRAWAL_APPROVE");
            }
            if (path.endsWith("/reject")) {
                return List.of("WITHDRAWAL_REJECT");
            }
            if ("GET".equals(method)) {
                return List.of("WITHDRAWAL_VIEW");
            }
            return List.of("WITHDRAWAL_APPROVE");
        }

        // 5. MODULE 4: BANNERS (/api/v1/admin/partners/banners**)
        if (path.startsWith("/api/v1/admin/partners/banners")) {
            if (path.endsWith("/approve") || path.endsWith("/reject")) {
                return List.of("BANNER_APPROVE_REJECT");
            }
            if ("GET".equals(method)) {
                return List.of("BANNER_VIEW");
            }
            return List.of("BANNER_CREATE_DELETE");
        }

        // 6. MODULE 5: IAM - USERS (/api/v1/admin/users**)
        if (path.startsWith("/api/v1/admin/users")) {
            if (path.endsWith("/status")) {
                return List.of("USER_STATUS_LOCK");
            }
            if (path.endsWith("/reset-password")) {
                return List.of("USER_PASSWORD_RESET");
            }
            if (path.endsWith("/role")) {
                return List.of("USER_ROLE_ASSIGN");
            }
            if ("GET".equals(method)) {
                return List.of("USER_VIEW");
            }
            return List.of("USER_CREATE_UPDATE_DELETE");
        }

        // 7. MODULE 5: IAM - ROLES (/api/v1/admin/roles**)
        if (path.startsWith("/api/v1/admin/roles")) {
            if ("GET".equals(method)) {
                return List.of("ROLE_MANAGE_CRUD", "USER_VIEW", "USER_CREATE_UPDATE_DELETE", "USER_ROLE_ASSIGN");
            }
            return List.of("ROLE_MANAGE_CRUD");
        }

        // 8. MODULE 6: BOOKINGS (/api/v1/admin/bookings**)
        if (path.startsWith("/api/v1/admin/bookings")) {
            if (path.endsWith("/refund")) {
                return List.of("BOOKING_FORCE_REFUND");
            }
            if ("GET".equals(method)) {
                return List.of("BOOKING_VIEW");
            }
            return List.of("BOOKING_FORCE_REFUND");
        }

        // 9. MODULE 7: STATIONS & TRIPS (/api/v1/admin/stations** & /api/v1/admin/trips**)
        if (path.startsWith("/api/v1/admin/stations")) {
            if ("GET".equals(method)) {
                return List.of("STATION_VIEW", "TRIP_MANAGE_CRUD");
            }
            return List.of("STATION_CREATE_UPDATE_DELETE");
        }
        if (path.startsWith("/api/v1/admin/trips")) {
            return List.of("TRIP_MANAGE_CRUD");
        }

        return List.of();
    }
}
