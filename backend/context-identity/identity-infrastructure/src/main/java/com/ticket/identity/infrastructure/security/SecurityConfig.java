package com.ticket.identity.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new AdminApiPermissionFilter(), AuthorizationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // 1. Guest & Public APIs (Strictly scoped)
                .requestMatchers("/api/v1/auth/me").authenticated()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/public/banners/active", "/api/v1/public/settings").permitAll()
                .requestMatchers("/api/v1/payments/webhook/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/notifications/telegram/webhook").permitAll()

                // Health probes: Allow minimal probe endpoint for container healthchecks (both dev /actuator/health and prod /internal/actuator/health)
                .requestMatchers(
                    "/actuator/health", "/actuator/health/**", "/actuator/info",
                    "/internal/actuator/health", "/internal/actuator/health/**", "/internal/actuator/info"
                ).permitAll()
                // BE-SEC-004: All other Actuator operational metrics require Super Admin (or explicit PERM_SYSTEM_SETTINGS_MANAGE)
                .requestMatchers("/actuator/**", "/internal/actuator/**").access((authentication, context) ->
                    new org.springframework.security.authorization.AuthorizationDecision(hasSuperAdminOrPermission(authentication.get(), "PERM_SYSTEM_SETTINGS_MANAGE"))
                )

                // Swagger / OpenAPI documentation (public in dev/staging; disabled via springdoc.api-docs.enabled in prod)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // 2. Authenticated Upload API (Prevent anonymous file abuse)
                .requestMatchers(HttpMethod.POST, "/api/v1/public/upload").authenticated()

                // 3. Administrator Only APIs
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // 4. Customer Role APIs
                .requestMatchers("/api/v1/bookings/**").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/payments/checkout").hasRole("CUSTOMER")

                // 5. Partner Role APIs (BE-SEC-004: Strictly scope /register to CUSTOMER/OPERATOR and /me GET to OPERATOR or ADMIN with PERM_OPERATOR_VIEW)
                .requestMatchers(HttpMethod.POST, "/api/v1/partners/register").hasAnyRole("CUSTOMER", "OPERATOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/partners/me").access((authentication, context) ->
                    new org.springframework.security.authorization.AuthorizationDecision(hasOperatorOrSuperAdminOrPermission(authentication.get(), "PERM_OPERATOR_VIEW"))
                )
                .requestMatchers("/api/v1/partners/**").hasRole("OPERATOR")

                // 6. Any other requests require authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication is required to access this resource\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth == null || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication is required to access this resource\"}");
                    } else {
                        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"success\":false,\"code\":\"ACCESS_DENIED\",\"message\":\"Access denied\"}");
                    }
                })
            );
        return http.build();
    }

    private static boolean hasSuperAdminOrPermission(org.springframework.security.core.Authentication auth, String requiredPerm) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return false;
        }
        java.util.Set<String> authorities = auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
        if (!authorities.contains("ROLE_ADMIN")) {
            return false;
        }
        boolean isCustomAdmin = authorities.stream().anyMatch(a -> a != null && a.startsWith("CUSTOM_ROLE_"));
        return !isCustomAdmin || authorities.contains(requiredPerm);
    }

    private static boolean hasOperatorOrSuperAdminOrPermission(org.springframework.security.core.Authentication auth, String requiredPerm) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return false;
        }
        java.util.Set<String> authorities = auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
        if (authorities.contains("ROLE_OPERATOR")) {
            return true;
        }
        return hasSuperAdminOrPermission(auth, requiredPerm);
    }
}
