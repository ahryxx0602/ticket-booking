package com.ticket.identity.infrastructure.security;

import com.ticket.identity.infrastructure.persistence.entity.SystemRoleJpaEntity;
import com.ticket.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.ticket.identity.infrastructure.persistence.repository.SpringDataSystemRoleRepository;
import com.ticket.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BE-SEC-006: IdentityDataSeeder Production Safety & Opt-In Bootstrap Tests")
class IdentityDataSeederTest {

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataSystemRoleRepository systemRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private IdentityDataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new IdentityDataSeeder(userRepository, passwordEncoder);
        seeder.setSystemRoleRepository(systemRoleRepository);
        lenient().when(systemRoleRepository.existsByCode(anyString())).thenReturn(false);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed_secret");
    }

    @Test
    @DisplayName("BE-SEC-006: Fresh Production startup must seed 5 system roles but ZERO demo user accounts")
    void run_WhenProdProfileAndBootstrapDisabled_ShouldSeedRolesOnlyAndZeroDemoUsers() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        seeder.setEnvironment(prodEnv);
        seeder.setDemoUsersEnabled(true); // Even if accidentally set true, prod profile must block demo seeding

        seeder.run();

        verify(systemRoleRepository, times(5)).save(any(SystemRoleJpaEntity.class));
        verify(userRepository, never()).save(any(UserJpaEntity.class));
    }

    @Test
    @DisplayName("BE-SEC-006: Production bootstrap with default or short password must fail fast with IllegalStateException")
    void run_WhenProdBootstrapHasWeakOrDefaultPassword_ShouldThrowIllegalStateException() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        seeder.setEnvironment(prodEnv);
        seeder.setDemoUsersEnabled(false);

        // 1. Default demo password -> rejected
        seeder.setBootstrapAdminConfig(true, "prod_admin", "admin@velobus.vn", "admin_dev_password");
        assertThatThrownBy(() -> seeder.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not use known default/demo passwords");

        // 2. Short password (< 12 chars) -> rejected
        seeder.setBootstrapAdminConfig(true, "prod_admin", "admin@velobus.vn", "Short123!");
        assertThatThrownBy(() -> seeder.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12 characters");
    }

    @Test
    @DisplayName("BE-SEC-006: Production opt-in bootstrap with strong credentials should provision single Super Admin account")
    void run_WhenProdBootstrapHasStrongCredentials_ShouldCreateSingleAdminUser() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        seeder.setEnvironment(prodEnv);
        seeder.setDemoUsersEnabled(false);
        seeder.setBootstrapAdminConfig(true, "velobus_root", "root@velobus.vn", "StrongProdBootstrapKey_2026_Valid!");

        when(userRepository.existsByUsername("velobus_root")).thenReturn(false);

        seeder.run();

        ArgumentCaptor<UserJpaEntity> userCaptor = ArgumentCaptor.forClass(UserJpaEntity.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("velobus_root");
        assertThat(userCaptor.getValue().getRole()).isEqualTo("ROLE_ADMIN");
        assertThat(userCaptor.getValue().getCustomRoleCode()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("BE-SEC-006: Dev profile with demo-users-enabled=true should continue seeding 3 dev demo accounts")
    void run_WhenDevProfileAndDemoEnabled_ShouldSeedThreeDemoUsers() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");
        seeder.setEnvironment(devEnv);
        seeder.setDemoUsersEnabled(true);

        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        seeder.run();

        verify(systemRoleRepository, times(5)).save(any(SystemRoleJpaEntity.class));
        verify(userRepository, times(3)).save(any(UserJpaEntity.class));
    }
}
