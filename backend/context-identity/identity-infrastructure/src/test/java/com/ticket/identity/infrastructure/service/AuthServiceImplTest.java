package com.ticket.identity.infrastructure.service;

import com.ticket.identity.application.dto.AuthRequest;
import com.ticket.identity.application.dto.AuthResponse;
import com.ticket.identity.application.dto.RegisterRequest;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.identity.infrastructure.security.JwtTokenProvider;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .username("customer1")
                .email("customer1@example.com")
                .fullName("Customer One")
                .passwordHash("hashed_password")
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("login: When valid credentials and active status, return AuthResponse")
    void login_Success() {
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("raw_password", "hashed_password")).thenReturn(true);
        when(tokenProvider.generateToken("customer1", "ROLE_CUSTOMER")).thenReturn("jwt_mock_token");

        AuthRequest request = new AuthRequest();
        request.setUsername("customer1");
        request.setPassword("raw_password");

        AuthResponse response = authService.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt_mock_token");
        assertThat(response.getUsername()).isEqualTo("customer1");
        assertThat(response.getRole()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("login: When account is LOCKED, must throw BusinessException ACCOUNT_LOCKED")
    void login_WhenAccountLocked_ShouldThrowException() {
        activeUser.setStatus(UserStatus.LOCKED);
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("raw_password", "hashed_password")).thenReturn(true);

        AuthRequest request = new AuthRequest();
        request.setUsername("customer1");
        request.setPassword("raw_password");

        assertThatThrownBy(() -> authService.execute(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("locked");
    }

    @Test
    @DisplayName("login: When password incorrect, throw BusinessException INVALID_CREDENTIALS")
    void login_WhenPasswordIncorrect_ShouldThrowException() {
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        AuthRequest request = new AuthRequest();
        request.setUsername("customer1");
        request.setPassword("wrong_password");

        assertThatThrownBy(() -> authService.execute(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    @DisplayName("register: When valid request, save user and return token")
    void register_Success() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("hashed_new_secret");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(tokenProvider.generateToken(eq("newuser"), eq("ROLE_CUSTOMER"))).thenReturn("jwt_new_token");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("Secret123");
        request.setFullName("New User");

        AuthResponse response = authService.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt_new_token");
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getRole()).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("login: When Custom Admin Role user logs in, embed customRole, roleName and permissions in AuthResponse and JWT")
    void login_WithCustomSystemRole_ShouldEmbedCustomRoleAndPermissions() {
        com.ticket.identity.domain.repository.SystemRoleRepository sysRoleRepo = org.mockito.Mockito.mock(
                com.ticket.identity.domain.repository.SystemRoleRepository.class
        );
        authService.setSystemRoleRepository(sysRoleRepo);

        User accountantUser = User.builder()
                .id(10L)
                .username("ketoan01")
                .email("ketoan@velobus.vn")
                .fullName("Ke Toan 01")
                .passwordHash("hashed_pwd")
                .role(UserRole.ROLE_ADMIN)
                .customRoleCode("ROLE_ACCOUNTANT")
                .status(UserStatus.ACTIVE)
                .build();

        com.ticket.identity.domain.model.SystemRole accountantRole = com.ticket.identity.domain.model.SystemRole.builder()
                .id(4L)
                .code("ROLE_ACCOUNTANT")
                .name("Kế Toán & Đối Soát")
                .category("SYSTEM")
                .permissions(java.util.List.of("PAYMENT_REPORT_VIEW", "WITHDRAWAL_VIEW"))
                .build();

        when(userRepository.findByUsername("ketoan01")).thenReturn(Optional.of(accountantUser));
        when(passwordEncoder.matches("raw_pwd", "hashed_pwd")).thenReturn(true);
        when(sysRoleRepo.findByCode("ROLE_ACCOUNTANT")).thenReturn(Optional.of(accountantRole));
        when(tokenProvider.generateToken(
                eq("ketoan01"),
                eq("ROLE_ADMIN"),
                eq("ROLE_ACCOUNTANT"),
                eq("Kế Toán & Đối Soát"),
                eq(java.util.List.of("PAYMENT_REPORT_VIEW", "WITHDRAWAL_VIEW"))
        )).thenReturn("jwt_accountant_token");

        AuthRequest req = new AuthRequest();
        req.setUsername("ketoan01");
        req.setPassword("raw_pwd");

        AuthResponse resp = authService.execute(req);

        assertThat(resp.getAccessToken()).isEqualTo("jwt_accountant_token");
        assertThat(resp.getRole()).isEqualTo("ROLE_ADMIN");
        assertThat(resp.getCustomRole()).isEqualTo("ROLE_ACCOUNTANT");
        assertThat(resp.getRoleName()).isEqualTo("Kế Toán & Đối Soát");
        assertThat(resp.getPermissions()).containsExactly("PAYMENT_REPORT_VIEW", "WITHDRAWAL_VIEW");
    }
}
