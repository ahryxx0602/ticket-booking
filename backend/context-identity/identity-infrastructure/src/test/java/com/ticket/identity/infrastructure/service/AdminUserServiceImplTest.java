package com.ticket.identity.infrastructure.service;

import com.ticket.identity.application.dto.AdminResetPasswordRequest;
import com.ticket.identity.application.dto.ChangeUserRoleRequest;
import com.ticket.identity.application.dto.UpdateUserStatusRequest;
import com.ticket.identity.application.dto.UserDetailResponse;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
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
}
