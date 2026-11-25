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
    private SystemRoleRepository systemRoleRepository;
    private UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Autowired(required = false)
    public void setSystemRoleRepository(SystemRoleRepository systemRoleRepository) {
        this.systemRoleRepository = systemRoleRepository;
    }

    @Autowired(required = false)
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
            if (!StringUtils.hasText(username) || jwtTokenProvider.isTokenRevokedForUser(username, token)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            String role = jwtTokenProvider.getRoleFromToken(token);
            String customRole = jwtTokenProvider.getCustomRoleFromToken(token);

            // BE-SEC-001: Đối chiếu trạng thái thực tế của User trong DB (nếu có UserRepository)
            // Thu hồi quyền ngay lập tức nếu tài khoản bị LOCKED, bị xóa hoặc bị thay đổi Role sau khi cấp JWT
            if (userRepository != null) {
                Optional<User> liveUserOpt = userRepository.findByUsername(username);
                if (liveUserOpt.isEmpty() || liveUserOpt.get().getStatus() == UserStatus.LOCKED) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                User liveUser = liveUserOpt.get();
                role = liveUser.getRole() != null ? liveUser.getRole().name() : "ROLE_CUSTOMER";
                customRole = liveUser.getEffectiveRoleCode();
            }

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

                List<String> effectivePerms = resolveLiveOrTokenPermissions(cleanCustomRole, token);
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
     * PC-03 & FND-005: Ưu tiên đọc permissions mới nhất từ DB (nếu có SystemRoleRepository).
     * Nếu Role đã bị xóa khỏi DB (roleOpt.isEmpty()), trả về emptyList() (thu hồi toàn bộ quyền) chứ không dùng lại JWT cũ.
     * Chỉ khi chạy trong unit test không inject SystemRoleRepository thì mới đọc từ JWT claims.
     */
    private List<String> resolveLiveOrTokenPermissions(String customRoleCode, String token) {
        if (systemRoleRepository != null) {
            Optional<SystemRole> roleOpt = systemRoleRepository.findByCode(customRoleCode);
            return roleOpt.map(SystemRole::getPermissions).orElse(Collections.emptyList());
        }
        return jwtTokenProvider.getPermissionsFromToken(token);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
