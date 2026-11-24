package com.ticket.identity.infrastructure.service;

import com.ticket.identity.application.dto.PermissionModuleCatalogDto;
import com.ticket.identity.application.dto.SystemRoleRequest;
import com.ticket.identity.application.dto.SystemRoleResponse;
import com.ticket.identity.domain.model.SystemRole;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.SystemRoleRepository;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.identity.infrastructure.security.AdminApiPermissionFilter;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminRoleServiceImpl & Dynamic RBAC Matrix Unit Tests")
class AdminRoleServiceImplTest {

    @Mock
    private SystemRoleRepository systemRoleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminRoleServiceImpl adminRoleService;

    @Test
    @DisplayName("getPermissionCatalog: Should return 7 modules with 26 API permissions")
    void getPermissionCatalog_ShouldReturn7ModulesAnd26Permissions() {
        List<PermissionModuleCatalogDto> catalog = adminRoleService.getPermissionCatalog();

        assertThat(catalog).hasSize(7);
        int totalPerms = catalog.stream()
                .mapToInt(m -> m.getPermissions().size())
                .sum();
        assertThat(totalPerms).isEqualTo(26);
    }

    @Test
    @DisplayName("createRole: Should normalize code, sanitize permissions and save new custom role")
    void createRole_WhenValid_ShouldNormalizeAndSave() {
        SystemRoleRequest req = SystemRoleRequest.builder()
                .code("accountant_lead")
                .name("Kế Toán Trưởng")
                .description("Đối soát và giải ngân")
                .category("SYSTEM")
                .permissions(List.of("PAYMENT_REPORT_VIEW", "WITHDRAWAL_VIEW", "WITHDRAWAL_APPROVE", "INVALID_FAKE_PERM"))
                .build();

        when(systemRoleRepository.existsByCode("ROLE_ACCOUNTANT_LEAD")).thenReturn(false);
        when(systemRoleRepository.save(any(SystemRole.class))).thenAnswer(inv -> {
            SystemRole r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });
        when(userRepository.countByRoleCode("ROLE_ACCOUNTANT_LEAD")).thenReturn(0L);

        SystemRoleResponse resp = adminRoleService.createRole(req, "admin");

        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getCode()).isEqualTo("ROLE_ACCOUNTANT_LEAD");
        assertThat(resp.getPermissions()).containsExactly("PAYMENT_REPORT_VIEW", "WITHDRAWAL_VIEW", "WITHDRAWAL_APPROVE");
        assertThat(resp.isSystemDefault()).isFalse();
    }

    @Test
    @DisplayName("updateRole: Should block editing system default roles (SYSTEM_ROLE_PROTECTED)")
    void updateRole_WhenSystemDefault_ShouldThrowBusinessException() {
        SystemRole defaultAdmin = SystemRole.builder()
                .id(1L)
                .code("ROLE_ADMIN")
                .name("Super Admin")
                .systemDefault(true)
                .build();

        when(systemRoleRepository.findById(1L)).thenReturn(Optional.of(defaultAdmin));

        SystemRoleRequest req = SystemRoleRequest.builder()
                .code("ROLE_ADMIN")
                .name("Hacked Admin")
                .permissions(List.of())
                .build();

        assertThatThrownBy(() -> adminRoleService.updateRole(1L, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("SYSTEM_ROLE_PROTECTED");
    }

    @Test
    @DisplayName("updateRole: Should block staff from modifying their own custom role (CANNOT_MODIFY_OWN_ROLE)")
    void updateRole_WhenModifyingOwnCustomRole_ShouldThrowForbiddenException() {
        SystemRole hrRole = SystemRole.builder()
                .id(5L)
                .code("ROLE_HR_MANAGER")
                .name("HR Manager")
                .systemDefault(false)
                .build();

        User caller = User.builder()
                .id(99L)
                .username("hr_staff")
                .role(UserRole.ROLE_ADMIN)
                .customRoleCode("ROLE_HR_MANAGER")
                .status(UserStatus.ACTIVE)
                .build();

        when(systemRoleRepository.findById(5L)).thenReturn(Optional.of(hrRole));
        when(userRepository.findByUsername("hr_staff")).thenReturn(Optional.of(caller));

        SystemRoleRequest req = SystemRoleRequest.builder()
                .code("ROLE_HR_MANAGER")
                .name("Elevated HR")
                .permissions(List.of("USER_VIEW", "ROLE_MANAGE_CRUD"))
                .build();

        assertThatThrownBy(() -> adminRoleService.updateRole(5L, req, "hr_staff"))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo("CANNOT_MODIFY_OWN_ROLE");
    }

    @Test
    @DisplayName("deleteRole: Should block deleting custom role currently assigned to users (ROLE_IN_USE)")
    void deleteRole_WhenInUse_ShouldThrowBusinessException() {
        SystemRole cskhRole = SystemRole.builder()
                .id(6L)
                .code("ROLE_CSKH")
                .name("CSKH")
                .systemDefault(false)
                .build();

        when(systemRoleRepository.findById(6L)).thenReturn(Optional.of(cskhRole));
        when(userRepository.countByRoleCode("ROLE_CSKH")).thenReturn(2L);

        assertThatThrownBy(() -> adminRoleService.deleteRole(6L, "admin"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("ROLE_IN_USE");
    }

    @Test
    @DisplayName("deleteRole: Should delete unused custom role successfully")
    void deleteRole_WhenUnused_ShouldDelete() {
        SystemRole tempRole = SystemRole.builder()
                .id(7L)
                .code("ROLE_TEMP")
                .name("Temp Role")
                .systemDefault(false)
                .build();

        when(systemRoleRepository.findById(7L)).thenReturn(Optional.of(tempRole));
        when(userRepository.countByRoleCode("ROLE_TEMP")).thenReturn(0L);

        adminRoleService.deleteRole(7L, "admin");
        verify(systemRoleRepository).deleteById(7L);
    }

    @Test
    @DisplayName("AdminApiPermissionFilter: Specificity-First matching maps all 26 APIs accurately")
    void specificityFirstMatching_ShouldMapSubPathsBeforeParentPaths() {
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("GET", "/api/v1/admin/payments/reports"))
                .containsExactly("PAYMENT_REPORT_VIEW");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("POST", "/api/v1/admin/payments/55/reconcile"))
                .containsExactly("PAYMENT_RECONCILE_RESOLVE");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("GET", "/api/v1/admin/payments"))
                .containsExactly("PAYMENT_LIST_VIEW");

        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("PATCH", "/api/v1/admin/users/10/status"))
                .containsExactly("USER_STATUS_LOCK");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("POST", "/api/v1/admin/users/10/reset-password"))
                .containsExactly("USER_PASSWORD_RESET");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("PATCH", "/api/v1/admin/users/10/role"))
                .containsExactly("USER_ROLE_ASSIGN");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("POST", "/api/v1/admin/users"))
                .containsExactly("USER_CREATE_UPDATE_DELETE");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("DELETE", "/api/v1/admin/users/10"))
                .containsExactly("USER_CREATE_UPDATE_DELETE");

        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("GET", "/api/v1/admin/roles"))
                .contains("ROLE_MANAGE_CRUD", "USER_VIEW", "USER_ROLE_ASSIGN");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("POST", "/api/v1/admin/roles"))
                .containsExactly("ROLE_MANAGE_CRUD");

        // Full 26 API permissions coverage check
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("GET", "/api/v1/admin/partners/operators"))
                .containsExactly("OPERATOR_VIEW");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("POST", "/api/v1/admin/partners/operators"))
                .containsExactly("OPERATOR_CREATE_UPDATE");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("PUT", "/api/v1/admin/partners/operators/1/approve"))
                .containsExactly("OPERATOR_APPROVE_REJECT");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("PUT", "/api/v1/admin/partners/operators/1/suspend"))
                .containsExactly("OPERATOR_SUSPEND");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("PUT", "/api/v1/admin/partners/operators/1/commission"))
                .containsExactly("OPERATOR_COMMISSION_MANAGE");

        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("GET", "/api/v1/admin/partners/withdrawals"))
                .containsExactly("WITHDRAWAL_VIEW");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("PUT", "/api/v1/admin/partners/withdrawals/1/approve"))
                .containsExactly("WITHDRAWAL_APPROVE");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("PUT", "/api/v1/admin/partners/withdrawals/1/reject"))
                .containsExactly("WITHDRAWAL_REJECT");

        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("GET", "/api/v1/admin/partners/banners"))
                .containsExactly("BANNER_VIEW");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("POST", "/api/v1/admin/partners/banners/platform"))
                .containsExactly("BANNER_CREATE_DELETE");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("PUT", "/api/v1/admin/partners/banners/1/approve"))
                .containsExactly("BANNER_APPROVE_REJECT");

        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("GET", "/api/v1/admin/bookings"))
                .containsExactly("BOOKING_VIEW");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("POST", "/api/v1/admin/bookings/BKG1/refund"))
                .containsExactly("BOOKING_FORCE_REFUND");

        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("GET", "/api/v1/admin/stations"))
                .contains("STATION_VIEW");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("POST", "/api/v1/admin/stations"))
                .containsExactly("STATION_CREATE_UPDATE_DELETE");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("POST", "/api/v1/admin/trips"))
                .containsExactly("TRIP_MANAGE_CRUD");
        assertThat(AdminApiPermissionFilter.resolveRequiredPermissions("PUT", "/api/v1/admin/settings"))
                .containsExactly("SYSTEM_SETTINGS_MANAGE");
    }
}
