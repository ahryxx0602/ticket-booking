package com.ticket.identity.infrastructure.service;

import com.ticket.identity.application.dto.*;
import com.ticket.identity.domain.model.SystemRole;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.SystemRoleRepository;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import com.ticket.shared.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserServiceImpl Unit Tests")
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SystemRoleRepository systemRoleRepository;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        adminUserService.setSystemRoleRepository(systemRoleRepository);
        sampleUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("testuser@example.com")
                .fullName("Test User")
                .passwordHash("hashed_old_password")
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("getUsers: Should return PageResponse with mapped UserDetailResponse")
    void getUsers_ShouldReturnPageResponse() {
        when(userRepository.searchUsers(eq("test"), eq("ROLE_CUSTOMER"), eq(UserStatus.ACTIVE), eq(0), eq(10)))
                .thenReturn(List.of(sampleUser));
        when(userRepository.countUsers(eq("test"), eq("ROLE_CUSTOMER"), eq(UserStatus.ACTIVE)))
                .thenReturn(1L);

        PageResponse<UserDetailResponse> result = adminUserService.execute("test", "ROLE_CUSTOMER", UserStatus.ACTIVE, 0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getUsername()).isEqualTo("testuser");
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getUserDetail: When user exists, should return details")
    void getUserDetail_WhenExists_ShouldReturnDetails() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserDetailResponse result = adminUserService.execute(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getUserDetail: When user not found, should throw EntityNotFoundException")
    void getUserDetail_WhenNotFound_ShouldThrowException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.execute(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User with id '99' not found");
    }

    @Test
    @DisplayName("updateUserStatus: Lock other user successfully")
    void updateUserStatus_LockOtherUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.LOCKED);

        UserDetailResponse result = adminUserService.execute(1L, request, "admin");

        assertThat(result.getStatus()).isEqualTo(UserStatus.LOCKED);
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("updateUserStatus: Admin locking self should throw BusinessException ADMIN_CANNOT_LOCK_SELF")
    void updateUserStatus_AdminLockingSelf_ShouldThrowException() {
        sampleUser.setUsername("admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.LOCKED);

        assertThatThrownBy(() -> adminUserService.execute(1L, request, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot lock your own");
    }

    @Test
    @DisplayName("changeUserRole: Change role of other user successfully")
    void changeUserRole_OtherUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChangeUserRoleRequest request = new ChangeUserRoleRequest();
        request.setRole("ADMIN");

        UserDetailResponse result = adminUserService.execute(1L, request, "admin");

        assertThat(result.getRole()).isEqualTo("ROLE_ADMIN");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("changeUserRole: Admin demoting self should throw BusinessException ADMIN_CANNOT_DEMOTE_SELF")
    void changeUserRole_AdminDemotingSelf_ShouldThrowException() {
        sampleUser.setUsername("admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        ChangeUserRoleRequest request = new ChangeUserRoleRequest();
        request.setRole("ROLE_CUSTOMER");

        assertThatThrownBy(() -> adminUserService.execute(1L, request, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot modify role of your own");
    }

    @Test
    @DisplayName("resetPassword: Hash password with BCrypt and save")
    void resetPassword_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("sample_new_password")).thenReturn("bcrypt_hashed_secret");

        AdminResetPasswordRequest request = new AdminResetPasswordRequest();
        request.setNewPassword("sample_new_password");

        adminUserService.execute(1L, request);

        assertThat(sampleUser.getPasswordHash()).isEqualTo("bcrypt_hashed_secret");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("createUser: Should create new user with Custom System Role (ROLE_ACCOUNTANT)")
    void createUser_WithCustomRole_ShouldAssignRoleAdminAndCustomRoleCode() {
        AdminCreateUserRequest req = AdminCreateUserRequest.builder()
                .username("ketoan_new")
                .email("ketoan_new@velobus.vn")
                .fullName("Nguyen Ke Toan")
                .phone("0901234567")
                .password("Password123")
                .role("ROLE_ACCOUNTANT")
                .build();

        when(userRepository.existsByUsername("ketoan_new")).thenReturn(false);
        when(userRepository.existsByEmail("ketoan_new@velobus.vn")).thenReturn(false);
        when(systemRoleRepository.findByCode("ROLE_ACCOUNTANT")).thenReturn(Optional.of(
                SystemRole.builder()
                        .id(4L)
                        .code("ROLE_ACCOUNTANT")
                        .name("Kế Toán & Đối Soát")
                        .category("SYSTEM")
                        .build()
        ));
        when(passwordEncoder.encode("Password123")).thenReturn("hashed_pwd");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(20L);
            return u;
        });

        UserDetailResponse resp = adminUserService.execute(req, "admin");

        assertThat(resp.getId()).isEqualTo(20L);
        assertThat(resp.getRole()).isEqualTo("ROLE_ACCOUNTANT");
        assertThat(resp.getCustomRole()).isEqualTo("ROLE_ACCOUNTANT");
        assertThat(resp.getCategory()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("updateUser & deleteUser: Should update user profile and guard against self-deletion / FK violation")
    void updateAndDeleteUser_ShouldEnforceGuards() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUpdateUserRequest updateReq = AdminUpdateUserRequest.builder()
                .fullName("Updated FullName")
                .email("testuser@example.com")
                .phone("0999888777")
                .build();

        UserDetailResponse updated = adminUserService.execute(1L, updateReq, "admin");
        assertThat(updated.getFullName()).isEqualTo("Updated FullName");

        // Self-delete guard
        sampleUser.setUsername("admin");
        assertThatThrownBy(() -> adminUserService.execute(1L, "admin"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("ADMIN_CANNOT_DELETE_SELF");

        // Safe delete FK violation guard
        sampleUser.setUsername("customer_with_bookings");
        doThrow(new DataIntegrityViolationException("FK constraint")).when(userRepository).deleteById(1L);
        assertThatThrownBy(() -> adminUserService.execute(1L, "admin"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("USER_HAS_LINKED_DATA");
    }

    @Test
    @DisplayName("SUPER_ADMIN_PROTECTED: Custom role staff cannot reset password, lock, delete or assign ROLE_ADMIN")
    void superAdminProtection_WhenCallerIsCustomRole_ShouldThrowForbiddenException() {
        User superAdmin = User.builder()
                .id(1L)
                .username("admin")
                .role(UserRole.ROLE_ADMIN)
                .customRoleCode("ROLE_ADMIN")
                .status(UserStatus.ACTIVE)
                .build();

        User cskhCaller = User.builder()
                .id(5L)
                .username("cskh01")
                .role(UserRole.ROLE_ADMIN)
                .customRoleCode("ROLE_CSKH")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(userRepository.findByUsername("cskh01")).thenReturn(Optional.of(cskhCaller));

        // 1. Custom role trying to reset Super Admin password -> FND-001 blocked!
        AdminResetPasswordRequest resetReq = new AdminResetPasswordRequest();
        resetReq.setNewPassword("hacked_password");
        assertThatThrownBy(() -> adminUserService.execute(1L, resetReq, "cskh01"))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo("SUPER_ADMIN_PROTECTED");

        // 2. Custom role trying to lock Super Admin -> blocked!
        UpdateUserStatusRequest lockReq = new UpdateUserStatusRequest();
        lockReq.setStatus(UserStatus.LOCKED);
        assertThatThrownBy(() -> adminUserService.execute(1L, lockReq, "cskh01"))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo("SUPER_ADMIN_PROTECTED");

        // 3. Custom role trying to assign ROLE_ADMIN to another user -> blocked!
        when(userRepository.findById(2L)).thenReturn(Optional.of(sampleUser));
        ChangeUserRoleRequest roleReq = new ChangeUserRoleRequest();
        roleReq.setRole("ROLE_ADMIN");
        assertThatThrownBy(() -> adminUserService.execute(2L, roleReq, "cskh01"))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo("SUPER_ADMIN_PROTECTED");
    }

    @Test
    @DisplayName("BE-SEC-002 & BE-SEC-005: Custom Admin without USER_ROLE_ASSIGN cannot change role via PUT /users/{id} and cannot assign role with higher permissions")
    void customAdmin_RoleAssignAndSubsetGuards_ShouldBlockPrivilegeEscalation() {
        User callerWithoutRoleAssign = User.builder()
                .id(10L)
                .username("staff_crud_only")
                .role(UserRole.ROLE_ADMIN)
                .customRoleCode("ROLE_USER_EDITOR")
                .status(UserStatus.ACTIVE)
                .build();

        SystemRole editorRole = SystemRole.builder()
                .id(11L)
                .code("ROLE_USER_EDITOR")
                .name("User Editor")
                .permissions(List.of("USER_VIEW", "USER_CREATE_UPDATE_DELETE"))
                .build();

        SystemRole accountantRole = SystemRole.builder()
                .id(4L)
                .code("ROLE_ACCOUNTANT")
                .name("Accountant")
                .permissions(List.of("PAYMENT_REPORT_VIEW", "WITHDRAWAL_APPROVE"))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByUsername("staff_crud_only")).thenReturn(Optional.of(callerWithoutRoleAssign));
        when(systemRoleRepository.findByCode("ROLE_USER_EDITOR")).thenReturn(Optional.of(editorRole));
        when(systemRoleRepository.findByCode("ROLE_ACCOUNTANT")).thenReturn(Optional.of(accountantRole));

        // 1. BE-SEC-005: Attempt to mutate user role via PUT /api/v1/admin/users/{id} without USER_ROLE_ASSIGN -> Blocked!
        AdminUpdateUserRequest updateWithRoleChange = AdminUpdateUserRequest.builder()
                .fullName("Updated Name")
                .email("testuser@example.com")
                .role("ROLE_ACCOUNTANT")
                .build();

        assertThatThrownBy(() -> adminUserService.execute(1L, updateWithRoleChange, "staff_crud_only"))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo("MISSING_ROLE_ASSIGN_PERMISSION");

        // 2. BE-SEC-002: Even if caller has USER_ROLE_ASSIGN, cannot assign ROLE_ACCOUNTANT containing WITHDRAWAL_APPROVE
        editorRole.setPermissions(List.of("USER_VIEW", "USER_CREATE_UPDATE_DELETE", "USER_ROLE_ASSIGN"));
        ChangeUserRoleRequest escalateRoleReq = new ChangeUserRoleRequest();
        escalateRoleReq.setRole("ROLE_ACCOUNTANT");

        assertThatThrownBy(() -> adminUserService.execute(1L, escalateRoleReq, "staff_crud_only"))
                .isInstanceOf(ForbiddenException.class)
                .extracting("code").isEqualTo("PRIVILEGE_ESCALATION_DETECTED");
    }
}
