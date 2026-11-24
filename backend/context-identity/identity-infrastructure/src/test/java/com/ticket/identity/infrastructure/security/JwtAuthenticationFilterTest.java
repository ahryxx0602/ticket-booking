package com.ticket.identity.infrastructure.security;

import com.ticket.identity.domain.model.SystemRole;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.SystemRoleRepository;
import com.ticket.identity.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BE-SEC-001: JwtAuthenticationFilter Immediate Revocation & Live Role Sync Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SystemRoleRepository systemRoleRepository;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
        filter.setUserRepository(userRepository);
        filter.setSystemRoleRepository(systemRoleRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("BE-SEC-001: Locked user with valid JWT must have authentication immediately revoked")
    void doFilterInternal_WhenUserIsLockedInDb_ShouldNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("valid.jwt.token")).thenReturn("locked_user");
        when(jwtTokenProvider.getRoleFromToken("valid.jwt.token")).thenReturn("ROLE_ADMIN");
        when(jwtTokenProvider.getCustomRoleFromToken("valid.jwt.token")).thenReturn("ROLE_ADMIN");

        User lockedUser = User.builder()
                .id(10L)
                .username("locked_user")
                .role(UserRole.ROLE_ADMIN)
                .customRoleCode("ROLE_ADMIN")
                .status(UserStatus.LOCKED)
                .build();
        when(userRepository.findByUsername("locked_user")).thenReturn(Optional.of(lockedUser));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("BE-SEC-001: Deleted user with valid JWT must have authentication immediately revoked")
    void doFilterInternal_WhenUserIsDeletedInDb_ShouldNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer deleted.user.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken("deleted.user.token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("deleted.user.token")).thenReturn("deleted_staff");
        when(userRepository.findByUsername("deleted_staff")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("BE-SEC-001: User demoted from ROLE_ADMIN to ROLE_CUSTOMER in DB immediately receives ROLE_CUSTOMER authority")
    void doFilterInternal_WhenUserRoleDemotedInDb_ShouldUseLiveDbRoleInsteadOfStaleJwtClaims() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer stale.admin.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken("stale.admin.token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("stale.admin.token")).thenReturn("former_staff");
        // Stale JWT still claims ROLE_ADMIN / ROLE_CSKH
        when(jwtTokenProvider.getRoleFromToken("stale.admin.token")).thenReturn("ROLE_ADMIN");
        when(jwtTokenProvider.getCustomRoleFromToken("stale.admin.token")).thenReturn("ROLE_CSKH");

        // Live DB has already demoted user to ROLE_CUSTOMER
        User demotedUser = User.builder()
                .id(15L)
                .username("former_staff")
                .role(UserRole.ROLE_CUSTOMER)
                .customRoleCode("ROLE_CUSTOMER")
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByUsername("former_staff")).thenReturn(Optional.of(demotedUser));

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authorities).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("BE-SEC-001: User reassigned to a different Custom Role in DB immediately receives the new role's permissions")
    void doFilterInternal_WhenUserReassignedToNewCustomRole_ShouldLoadNewCustomRolePermissions() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer reassigned.staff.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken("reassigned.staff.token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("reassigned.staff.token")).thenReturn("staff01");
        when(jwtTokenProvider.getRoleFromToken("reassigned.staff.token")).thenReturn("ROLE_ADMIN");
        when(jwtTokenProvider.getCustomRoleFromToken("reassigned.staff.token")).thenReturn("ROLE_ACCOUNTANT");

        // Live DB reassigned staff01 to ROLE_BANNER_MOD
        User liveUser = User.builder()
                .id(16L)
                .username("staff01")
                .role(UserRole.ROLE_ADMIN)
                .customRoleCode("ROLE_BANNER_MOD")
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(liveUser));

        SystemRole bannerRole = SystemRole.builder()
                .id(9L)
                .code("ROLE_BANNER_MOD")
                .name("Banner Moderator")
                .permissions(List.of("BANNER_VIEW", "BANNER_APPROVE_REJECT"))
                .build();
        when(systemRoleRepository.findByCode("ROLE_BANNER_MOD")).thenReturn(Optional.of(bannerRole));

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authorities).containsExactlyInAnyOrder(
                "ROLE_ADMIN",
                "CUSTOM_ROLE_ROLE_BANNER_MOD",
                "PERM_BANNER_VIEW",
                "PERM_BANNER_APPROVE_REJECT"
        );
    }
}
