package com.ticket.identity.infrastructure.service;

import com.ticket.identity.application.dto.*;
import com.ticket.identity.application.usecase.*;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements
        AdminGetUsersUseCase,
        AdminGetUserDetailUseCase,
        AdminCreateUserUseCase,
        AdminUpdateUserUseCase,
        AdminDeleteUserUseCase,
        AdminUpdateUserStatusUseCase,
        AdminChangeUserRoleUseCase,
        AdminResetPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private SystemRoleRepository systemRoleRepository;

    @Autowired(required = false)
    public void setSystemRoleRepository(SystemRoleRepository systemRoleRepository) {
        this.systemRoleRepository = systemRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDetailResponse> execute(String keyword, String role, UserStatus status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 10 : size;

        String normalizedRoleFilter = mapCategoryOrRoleFilter(role);

        List<User> users = userRepository.searchUsers(keyword, normalizedRoleFilter, status, safePage, safeSize);
        long total = userRepository.countUsers(keyword, normalizedRoleFilter, status);

        // FND-007: Tải trước map Role Code -> Display Name 1 lần duy nhất chống N+1 query
        Map<String, String> roleNameMap = loadRoleDisplayNameMap();

        List<UserDetailResponse> responses = users.stream()
                .map(u -> toResponse(u, roleNameMap))
                .toList();

        return PageResponse.of(responses, safePage, safeSize, total);
    }

    private String mapCategoryOrRoleFilter(String roleOrCategory) {
        if (roleOrCategory == null || roleOrCategory.isBlank() || "ALL".equalsIgnoreCase(roleOrCategory)) {
            return null;
        }
        String trimmed = roleOrCategory.trim().toUpperCase();
        if ("CUSTOMER".equals(trimmed)) {
            return "ROLE_CUSTOMER";
        }
        if ("OPERATOR".equals(trimmed)) {
            return "ROLE_OPERATOR";
        }
        if ("SYSTEM".equals(trimmed)) {
            return "CATEGORY_SYSTEM";
        }
        return trimmed;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse execute(Long id) {
        User user = findUserOrThrow(id);
        return toResponse(user, null);
    }

    @Override
    @Transactional
    public UserDetailResponse execute(AdminCreateUserRequest request, String currentAdminUsername) {
        String cleanUsername = request.getUsername().trim();
        String cleanEmail = request.getEmail().trim();

        if (userRepository.existsByUsername(cleanUsername)) {
            throw new BusinessException("USERNAME_EXISTS", "Tên đăng nhập đã tồn tại: " + cleanUsername);
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new BusinessException("EMAIL_EXISTS", "Email đã được sử dụng: " + cleanEmail);
        }

        ResolvedRole resolved = resolveTargetRole(request.getRole());
        enforceSuperAdminCreationGuard(resolved.customRoleCode(), currentAdminUsername);
        // BE-SEC-005 & BE-SEC-002: Nếu tạo user với Role khác ROLE_CUSTOMER, bắt buộc kiểm tra quyền USER_ROLE_ASSIGN và Subset Permissions
        if (!"ROLE_CUSTOMER".equalsIgnoreCase(resolved.customRoleCode())) {
            enforceCallerHasRoleAssignPermission(currentAdminUsername);
            enforceTargetRolePermissionSubset(resolved.customRoleCode(), currentAdminUsername);
        }

        User newUser = User.builder()
                .username(cleanUsername)
                .email(cleanEmail)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(resolved.baseRole())
                .customRoleCode(resolved.customRoleCode())
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(newUser);
        log.info("[ADMIN USER] Created new user {} with role {} (customRole={}) by {}",
                saved.getUsername(), saved.getRole(), saved.getEffectiveRoleCode(), currentAdminUsername);
        return toResponse(saved, null);
    }

    @Override
    @Transactional
    public UserDetailResponse execute(Long id, AdminUpdateUserRequest request, String currentAdminUsername) {
        User user = findUserOrThrow(id);
        enforceTargetSuperAdminProtection(user, currentAdminUsername);
        enforceTargetUserPermissionSubset(user, currentAdminUsername);

        String cleanEmail = request.getEmail().trim();
        if (!cleanEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(cleanEmail)) {
            throw new BusinessException("EMAIL_EXISTS", "Email đã được sử dụng bởi tài khoản khác: " + cleanEmail);
        }

        user.updateProfile(request.getFullName(), cleanEmail, request.getPhone());

        if (request.getRole() != null && !request.getRole().isBlank()) {
            ResolvedRole resolved = resolveTargetRole(request.getRole());
            if (!resolved.customRoleCode().equalsIgnoreCase(user.getEffectiveRoleCode())) {
                if (user.getUsername().equalsIgnoreCase(currentAdminUsername)) {
                    throw new BusinessException("ADMIN_CANNOT_DEMOTE_SELF", "You cannot modify role of your own account");
                }
                enforceCallerHasRoleAssignPermission(currentAdminUsername);
                enforceSuperAdminCreationGuard(resolved.customRoleCode(), currentAdminUsername);
                enforceTargetRolePermissionSubset(resolved.customRoleCode(), currentAdminUsername);
                user.assignSystemRole(resolved.baseRole(), resolved.customRoleCode());
            }
        }

        User saved = userRepository.save(user);
        log.info("[ADMIN USER] Updated profile of user {} by {}", saved.getUsername(), currentAdminUsername);
        return toResponse(saved, null);
    }

    @Override
    @Transactional
    public void execute(Long id, String currentAdminUsername) {
        User user = findUserOrThrow(id);

        if (user.getUsername().equalsIgnoreCase(currentAdminUsername)) {
            throw new BusinessException("ADMIN_CANNOT_DELETE_SELF", "Bạn không thể tự xóa tài khoản quản trị đang đăng nhập của chính mình");
        }
        enforceTargetSuperAdminProtection(user, currentAdminUsername);
        enforceTargetUserPermissionSubset(user, currentAdminUsername);

        try {
            userRepository.deleteById(user.getId());
            log.info("[ADMIN USER] Deleted user {} (id={}) by {}", user.getUsername(), id, currentAdminUsername);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("USER_HAS_LINKED_DATA",
                    "Tài khoản này đã phát sinh đơn đặt vé hoặc hồ sơ nhà xe liên kết. Vui lòng sử dụng chức năng 'Khóa tài khoản' thay vì xóa cứng để bảo toàn lịch sử đối soát.");
        }
    }

    @Override
    @Transactional
    public UserDetailResponse execute(Long id, UpdateUserStatusRequest request, String currentAdminUsername) {
        User user = findUserOrThrow(id);

        if (user.getUsername().equalsIgnoreCase(currentAdminUsername) && request.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException("ADMIN_CANNOT_LOCK_SELF", "You cannot lock your own administrator account");
        }
        enforceTargetSuperAdminProtection(user, currentAdminUsername);
        enforceTargetUserPermissionSubset(user, currentAdminUsername);

        if (request.getStatus() == UserStatus.LOCKED) {
            user.lock();
            log.info("[ADMIN USER] User {} has been LOCKED by admin {}", user.getUsername(), currentAdminUsername);
        } else if (request.getStatus() == UserStatus.ACTIVE) {
            user.activate();
            log.info("[ADMIN USER] User {} has been ACTIVATED by admin {}", user.getUsername(), currentAdminUsername);
        } else {
            user.setStatus(request.getStatus());
        }

        User updated = userRepository.save(user);
        return toResponse(updated, null);
    }

    @Override
    @Transactional
    public UserDetailResponse execute(Long id, ChangeUserRoleRequest request, String currentAdminUsername) {
        User user = findUserOrThrow(id);

        if (user.getUsername().equalsIgnoreCase(currentAdminUsername)) {
            throw new BusinessException("ADMIN_CANNOT_DEMOTE_SELF", "You cannot modify role of your own account");
        }
        enforceTargetSuperAdminProtection(user, currentAdminUsername);
        enforceTargetUserPermissionSubset(user, currentAdminUsername);

        ResolvedRole resolved = resolveTargetRole(request.getRole());
        enforceSuperAdminCreationGuard(resolved.customRoleCode(), currentAdminUsername);
        enforceCallerHasRoleAssignPermission(currentAdminUsername);
        enforceTargetRolePermissionSubset(resolved.customRoleCode(), currentAdminUsername);

        user.assignSystemRole(resolved.baseRole(), resolved.customRoleCode());
        User updated = userRepository.save(user);
        log.info("[ADMIN USER] Role of user {} changed to {} (base={}) by admin {}",
                user.getUsername(), resolved.customRoleCode(), resolved.baseRole(), currentAdminUsername);
        return toResponse(updated, null);
    }

    @Override
    @Transactional
    public void execute(Long id, AdminResetPasswordRequest request) {
        execute(id, request, resolveAuthenticatedUsername());
    }

    @Override
    @Transactional
    public void execute(Long id, AdminResetPasswordRequest request, String currentAdminUsername) {
        User user = findUserOrThrow(id);
        // FND-001 & BE-SEC-002: Chốt chặn bảo vệ tài khoản Super Admin và tài khoản có quyền cao hơn trước thao tác Reset Password
        enforceTargetSuperAdminProtection(user, currentAdminUsername);
        enforceTargetUserPermissionSubset(user, currentAdminUsername);

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("[ADMIN USER] Password of user {} reset successfully by admin {}", user.getUsername(), currentAdminUsername);
    }

    private String resolveAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : null;
    }

    private record ResolvedRole(UserRole baseRole, String customRoleCode, String roleName) {}

    private ResolvedRole resolveTargetRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            throw new BusinessException("INVALID_USER_ROLE", "Role cannot be null or empty");
        }
        String normalized = requestedRole.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        if ("ROLE_CUSTOMER".equals(normalized)) {
            return new ResolvedRole(UserRole.ROLE_CUSTOMER, "ROLE_CUSTOMER", "Khách Hàng Đặt Vé");
        }
        if ("ROLE_OPERATOR".equals(normalized)) {
            return new ResolvedRole(UserRole.ROLE_OPERATOR, "ROLE_OPERATOR", "Chủ Xe / Đối Tác Vận Tải");
        }
        if ("ROLE_ADMIN".equals(normalized)) {
            return new ResolvedRole(UserRole.ROLE_ADMIN, "ROLE_ADMIN", "Quản Trị Viên Toàn Sàn (Super Admin)");
        }

        if (systemRoleRepository != null) {
            Optional<SystemRole> customOpt = systemRoleRepository.findByCode(normalized);
            if (customOpt.isPresent()) {
                SystemRole sr = customOpt.get();
                return new ResolvedRole(sr.toBaseUserRole(), sr.getCode(), sr.getName());
            }
        }

        throw new BusinessException("INVALID_USER_ROLE",
                "Invalid role: '" + requestedRole + "'. Role does not exist in system_roles.");
    }

    /**
     * PC-02 & FND-001: Nhân sự mang Custom Role không phải Super Admin không được phép thao tác lên tài khoản ROLE_ADMIN
     */
    private void enforceTargetSuperAdminProtection(User targetUser, String currentAdminUsername) {
        if (targetUser.isSuperAdmin() && !isCallerSuperAdmin(currentAdminUsername)) {
            throw new ForbiddenException("SUPER_ADMIN_PROTECTED",
                    "Nhân sự mang Role tùy chỉnh không được phép sửa, khóa, đặt lại mật khẩu hoặc xóa tài khoản Super Admin (ROLE_ADMIN)");
        }
    }

    /**
     * PC-02: Nhân sự mang Custom Role không phải Super Admin không được phép tự gán hoặc tạo mới ROLE_ADMIN
     */
    private void enforceSuperAdminCreationGuard(String targetRoleCode, String currentAdminUsername) {
        if ("ROLE_ADMIN".equalsIgnoreCase(targetRoleCode) && !isCallerSuperAdmin(currentAdminUsername)) {
            throw new ForbiddenException("SUPER_ADMIN_PROTECTED",
                    "Chỉ Super Admin (ROLE_ADMIN) mới có quyền cấp phát hoặc gán quyền ROLE_ADMIN cho tài khoản khác");
        }
    }

    /**
     * BE-SEC-005: Ngăn nhân sự chỉ có USER_CREATE_UPDATE_DELETE (nhưng không có USER_ROLE_ASSIGN) tự ý gán/đổi Role qua POST/PUT user.
     */
    private void enforceCallerHasRoleAssignPermission(String currentAdminUsername) {
        if (isCallerSuperAdmin(currentAdminUsername)) {
            return;
        }
        java.util.Set<String> callerPerms = resolveCallerPermissionsIfCustomAdmin(currentAdminUsername);
        if (callerPerms != null && !callerPerms.contains("USER_ROLE_ASSIGN")) {
            throw new ForbiddenException("MISSING_ROLE_ASSIGN_PERMISSION",
                    "Bạn cần quyền USER_ROLE_ASSIGN để gán hoặc thay đổi vai trò của người dùng");
        }
    }

    /**
     * BE-SEC-002: Ngăn nhân sự Custom Role gán một Custom Role có quyền cao hơn mình cho người khác.
     */
    private void enforceTargetRolePermissionSubset(String targetRoleCode, String currentAdminUsername) {
        if (targetRoleCode == null || "ROLE_CUSTOMER".equalsIgnoreCase(targetRoleCode) || "ROLE_OPERATOR".equalsIgnoreCase(targetRoleCode)) {
            return;
        }
        if (isCallerSuperAdmin(currentAdminUsername)) {
            return;
        }
        java.util.Set<String> callerPerms = resolveCallerPermissionsIfCustomAdmin(currentAdminUsername);
        if (callerPerms == null || systemRoleRepository == null) {
            return;
        }
        Optional<SystemRole> targetRoleOpt = systemRoleRepository.findByCode(targetRoleCode.trim().toUpperCase());
        if (targetRoleOpt.isPresent()) {
            for (String perm : targetRoleOpt.get().getPermissions()) {
                if (perm != null && !callerPerms.contains(perm.trim().toUpperCase())) {
                    throw new ForbiddenException("PRIVILEGE_ESCALATION_DETECTED",
                            "Bạn không thể gán vai trò '" + targetRoleCode + "' vì vai trò này chứa quyền '" + perm + "' vượt quá quyền hiện tại của bạn");
                }
            }
        }
    }

    /**
     * BE-SEC-002: Ngăn nhân sự Custom Role sửa/khóa/xóa/reset mật khẩu của một nhân sự hệ thống khác có quyền mà caller không sở hữu.
     */
    private void enforceTargetUserPermissionSubset(User targetUser, String currentAdminUsername) {
        if (targetUser == null || !"SYSTEM".equalsIgnoreCase(targetUser.getCategory())) {
            return;
        }
        if (targetUser.getUsername() != null && targetUser.getUsername().equalsIgnoreCase(currentAdminUsername)) {
            return;
        }
        enforceTargetRolePermissionSubset(targetUser.getEffectiveRoleCode(), currentAdminUsername);
    }

    private java.util.Set<String> resolveCallerPermissionsIfCustomAdmin(String currentAdminUsername) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            boolean hasCustomRoleAuthority = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a != null && a.startsWith("CUSTOM_ROLE_"));
            if (hasCustomRoleAuthority) {
                return auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(a -> a != null && a.startsWith("PERM_"))
                        .map(a -> a.substring(5))
                        .collect(java.util.stream.Collectors.toSet());
            }
        }
        if (currentAdminUsername != null && !currentAdminUsername.isBlank()) {
            Optional<User> callerOpt = userRepository.findByUsername(currentAdminUsername);
            if (callerOpt.isPresent() && !callerOpt.get().isSuperAdmin()) {
                if (systemRoleRepository != null) {
                    return systemRoleRepository.findByCode(callerOpt.get().getEffectiveRoleCode())
                            .map(sr -> new java.util.HashSet<>(sr.getPermissions()))
                            .orElseGet(java.util.HashSet::new);
                }
                return new java.util.HashSet<>();
            }
        }
        return null;
    }

    private boolean isCallerSuperAdmin(String currentAdminUsername) {
        // 1. Kiểm tra SecurityContextHolder trước: nếu Authentication có CUSTOM_ROLE_*, chắc chắn KHÔNG phải Super Admin
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            boolean hasCustomRoleAuthority = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a != null && a.startsWith("CUSTOM_ROLE_"));
            if (hasCustomRoleAuthority) {
                return false;
            }
        }

        // 2. Đối chiếu DB theo currentAdminUsername
        if (currentAdminUsername != null && !currentAdminUsername.isBlank()) {
            Optional<User> callerOpt = userRepository.findByUsername(currentAdminUsername);
            if (callerOpt.isPresent()) {
                return callerOpt.get().isSuperAdmin();
            }
        }
        return true;
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    private Map<String, String> loadRoleDisplayNameMap() {
        Map<String, String> map = new HashMap<>();
        map.put("ROLE_ADMIN", "Quản Trị Viên Toàn Sàn (Super Admin)");
        map.put("ROLE_OPERATOR", "Chủ Xe / Đối Tác Vận Tải");
        map.put("ROLE_CUSTOMER", "Khách Hàng Đặt Vé");
        map.put("ROLE_ACCOUNTANT", "Kế Toán & Đối Soát Tài Chính");
        map.put("ROLE_CSKH", "CSKH & Điều Hành Vận Tải");
        if (systemRoleRepository != null) {
            for (SystemRole sr : systemRoleRepository.findAll()) {
                map.put(sr.getCode(), sr.getName());
            }
        }
        return map;
    }

    private UserDetailResponse toResponse(User user, Map<String, String> preloadedRoleNameMap) {
        String effectiveCustomRole = user.getEffectiveRoleCode();
        String displayRoleName = preloadedRoleNameMap != null
                ? preloadedRoleNameMap.getOrDefault(effectiveCustomRole, effectiveCustomRole)
                : resolveRoleDisplayName(effectiveCustomRole);

        return UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(effectiveCustomRole)
                .customRole(effectiveCustomRole)
                .roleName(displayRoleName)
                .category(user.getCategory())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private String resolveRoleDisplayName(String roleCode) {
        if (systemRoleRepository != null) {
            Optional<SystemRole> sr = systemRoleRepository.findByCode(roleCode);
            if (sr.isPresent()) {
                return sr.get().getName();
            }
        }
        return switch (roleCode) {
            case "ROLE_ADMIN" -> "Quản Trị Viên Toàn Sàn (Super Admin)";
            case "ROLE_OPERATOR" -> "Chủ Xe / Đối Tác Vận Tải";
            case "ROLE_CUSTOMER" -> "Khách Hàng Đặt Vé";
            case "ROLE_ACCOUNTANT" -> "Kế Toán & Đối Soát Tài Chính";
            case "ROLE_CSKH" -> "CSKH & Điều Hành Vận Tải";
            default -> roleCode;
        };
    }
}
