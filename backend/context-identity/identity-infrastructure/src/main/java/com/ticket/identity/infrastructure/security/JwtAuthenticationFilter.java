package com.ticket.identity.infrastructure.security;

import com.ticket.identity.domain.model.SystemRole;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.SystemRoleRepository;
import com.ticket.identity.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private UserRepository userRepository;
    private SystemRoleRepository systemRoleRepository;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserRepository userRepository,
                                   SystemRoleRepository systemRoleRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.systemRoleRepository = systemRoleRepository;
    }

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void setSystemRoleRepository(SystemRoleRepository systemRoleRepository) {
        this.systemRoleRepository = systemRoleRepository;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            if (!StringUtils.hasText(username)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // BE-SEC-001: Bắt buộc đối chiếu trạng thái thực tế của User và persistent tokenVersion trong DB
            if (userRepository == null) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            Optional<User> liveUserOpt = userRepository.findByUsername(username);
            if (liveUserOpt.isEmpty() || liveUserOpt.get().getStatus() != UserStatus.ACTIVE) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            User liveUser = liveUserOpt.get();
            long tokenVersion = jwtTokenProvider.getTokenVersionFromToken(token);
            if (tokenVersion != liveUser.getEffectiveTokenVersion()) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            String role = liveUser.getRole() != null ? liveUser.getRole().name() : "ROLE_CUSTOMER";
            String customRole = liveUser.getEffectiveRoleCode();

            String baseRole = (role != null && !role.isBlank()) ? role : "ROLE_CUSTOMER";
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(baseRole));

            // Nếu là nhân sự hệ thống mang Custom Role (không phải Super Admin ROLE_ADMIN)
            if ("ROLE_ADMIN".equalsIgnoreCase(baseRole)
                    && customRole != null
                    && !customRole.isBlank()
                    && !"ROLE_ADMIN".equalsIgnoreCase(customRole)) {
                String cleanCustomRole = customRole.trim().toUpperCase();
                authorities.add(new SimpleGrantedAuthority("CUSTOM_ROLE_" + cleanCustomRole));

                List<String> effectivePerms = resolveLivePermissions(cleanCustomRole);
                for (String perm : effectivePerms) {
                    if (perm != null && !perm.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("PERM_" + perm.trim().toUpperCase()));
                    }
                }
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * BE-SEC-001: Chỉ đọc permissions mới nhất từ SystemRoleRepository trong DB.
     * Nếu Role đã bị xóa khỏi DB hoặc repository chưa cấu hình -> trả về emptyList() (Fail-Closed).
     */
    private List<String> resolveLivePermissions(String customRoleCode) {
        if (systemRoleRepository == null) {
            return Collections.emptyList();
        }
        Optional<SystemRole> roleOpt = systemRoleRepository.findByCode(customRoleCode);
        return roleOpt.map(SystemRole::getPermissions).orElse(Collections.emptyList());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
