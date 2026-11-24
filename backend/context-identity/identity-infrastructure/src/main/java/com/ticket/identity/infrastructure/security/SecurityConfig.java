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
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/public/banners/active", "/api/v1/public/settings").permitAll()
                .requestMatchers("/api/v1/payments/webhook/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/notifications/telegram/webhook").permitAll()

                // Health probes: Allow minimal probe endpoint for container healthchecks
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/internal/actuator/health/**").permitAll()
                // All other Actuator operational metrics require ADMIN
                .requestMatchers("/actuator/**", "/internal/actuator/**").hasRole("ADMIN")

                // Swagger / OpenAPI documentation (public in dev/staging; disabled via springdoc.api-docs.enabled in prod)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // 2. Authenticated Upload API (Prevent anonymous file abuse)
                .requestMatchers(HttpMethod.POST, "/api/v1/public/upload").authenticated()

                // 3. Administrator Only APIs
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // 4. Customer Role APIs
                .requestMatchers("/api/v1/bookings/**").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/payments/checkout").hasRole("CUSTOMER")

                // 5. Partner Role APIs
                .requestMatchers("/api/v1/partners/register").authenticated()
                .requestMatchers("/api/v1/partners/**").hasAnyRole("OPERATOR", "ADMIN")

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
}
