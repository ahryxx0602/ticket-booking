package com.ticket.identity.infrastructure.service;

import com.ticket.identity.application.dto.PermissionModuleCatalogDto;
import com.ticket.identity.application.dto.SystemRoleRequest;
import com.ticket.identity.application.dto.SystemRoleResponse;
import com.ticket.identity.application.usecase.AdminRoleManagementUseCase;
import com.ticket.identity.domain.model.AdminPermissionCatalog;
import com.ticket.identity.domain.model.SystemRole;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.repository.SystemRoleRepository;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import com.ticket.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleManagementUseCase {

    private final SystemRoleRepository systemRoleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionModuleCatalogDto> getPermissionCatalog() {
        return AdminPermissionCatalog.MODULES.stream()
                .map(mod -> PermissionModuleCatalogDto.builder()
                        .moduleCode(mod.moduleCode())
                        .moduleName(mod.moduleName())
                        .routePath(mod.routePath())
                        .viewPermissionCode(mod.viewPermissionCode())
                        .permissions(mod.permissions().stream()
                                .map(p -> PermissionModuleCatalogDto.PermissionItemDto.builder()
                                        .code(p.code())
                                        .name(p.name())
                                        .httpMethod(p.httpMethod())
                                        .apiPattern(p.apiPattern())
                                        .description(p.description())
                                        .viewPermission(p.viewPermission())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemRoleResponse> getAllRoles() {
        return systemRoleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SystemRoleResponse createRole(SystemRoleRequest request, String currentAdminUsername) {
        String normalizedCode = SystemRole.normalizeCode(request.getCode());
        if (systemRoleRepository.existsByCode(normalizedCode)) {
            throw new BusinessException("ROLE_CODE_EXISTS", "Mã Role đã tồn tại trên hệ thống: " + normalizedCode);
        }

        SystemRole role = SystemRole.builder()
                .code(normalizedCode)
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory() != null && !request.getCategory().isBlank() ? request.getCategory() : "SYSTEM")
                .permissions(request.getPermissions())
                .systemDefault(false)
                .build();
        role.validateAndNormalize();

        // BE-SEC-002: Chống leo thang đặc quyền — Custom Admin không được tạo Role chứa quyền mà chính họ không có
        enforcePermissionSubsetForCaller(role.getPermissions(), currentAdminUsername);

        SystemRole saved = systemRoleRepository.save(role);
        log.info("[ADMIN ROLE] Created custom role {} ({} permissions) by {}",
                saved.getCode(), saved.getPermissions().size(), currentAdminUsername);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SystemRoleResponse updateRole(Long id, SystemRoleRequest request, String currentAdminUsername) {
        SystemRole existing = systemRoleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SystemRole", id));

        if (existing.isSystemDefault()) {
            throw new BusinessException("SYSTEM_ROLE_PROTECTED",
                    "Không thể chỉnh sửa 3 Role mặc định gốc của hệ thống (" + existing.getCode() + ")");
        }

        enforceCannotModifyOwnCustomRole(existing.getCode(), currentAdminUsername);
        // BE-SEC-002: Không được chỉnh sửa một Role đang có quyền cao hơn quyền hiện tại của caller
        enforcePermissionSubsetForCaller(existing.getPermissions(), currentAdminUsername);

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            existing.setCategory(request.getCategory());
        }
        existing.setPermissions(request.getPermissions());
        existing.validateAndNormalize();

        // BE-SEC-002: Tập quyền mới gán vào Role cũng phải là tập con của quyền hiện tại của caller
        enforcePermissionSubsetForCaller(existing.getPermissions(), currentAdminUsername);

        SystemRole saved = systemRoleRepository.save(existing);
        log.info("[ADMIN ROLE] Updated custom role {} ({} permissions) by {}",
                saved.getCode(), saved.getPermissions().size(), currentAdminUsername);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRole(Long id, String currentAdminUsername) {
        SystemRole existing = systemRoleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SystemRole", id));

        if (existing.isSystemDefault()) {
            throw new BusinessException("SYSTEM_ROLE_PROTECTED",
                    "Không thể xóa 3 Role mặc định gốc của hệ thống (" + existing.getCode() + ")");
        }

        enforceCannotModifyOwnCustomRole(existing.getCode(), currentAdminUsername);
        enforcePermissionSubsetForCaller(existing.getPermissions(), currentAdminUsername);

        long assignedUsers = userRepository.countByRoleCode(existing.getCode());
        if (assignedUsers > 0) {
            throw new BusinessException("ROLE_IN_USE",
                    "Không thể xóa Role '" + existing.getCode() + "' vì đang có " + assignedUsers + " tài khoản được gán Role này. Vui lòng đổi Role cho các tài khoản đó trước.");
        }

        systemRoleRepository.deleteById(existing.getId());
        log.info("[ADMIN ROLE] Deleted custom role {} by {}", existing.getCode(), currentAdminUsername);
    }

    /**
     * PC-02: Nhân sự mang Custom Role không được phép tự sửa hoặc xóa chính Role mình đang mang
     */
    private void enforceCannotModifyOwnCustomRole(String targetRoleCode, String currentAdminUsername) {
        if (currentAdminUsername == null || currentAdminUsername.isBlank()) {
            return;
        }
        Optional<User> callerOpt = userRepository.findByUsername(currentAdminUsername);
        if (callerOpt.isPresent()) {
            User caller = callerOpt.get();
            if (!caller.isSuperAdmin() && targetRoleCode.equalsIgnoreCase(caller.getEffectiveRoleCode())) {
                throw new ForbiddenException("CANNOT_MODIFY_OWN_ROLE",
                        "Bạn không được phép tự chỉnh sửa quyền hạn hoặc xóa chính Role mà tài khoản của bạn đang sử dụng");
            }
        }
    }

    /**
     * BE-SEC-002: Nhân sự mang Custom Role chỉ được phép tạo/sửa/xóa các Role mà tập quyền là tập con (subset) của quyền họ đang sở hữu.
     */
    private void enforcePermissionSubsetForCaller(List<String> targetPermissions, String currentAdminUsername) {
        if (targetPermissions == null || targetPermissions.isEmpty()) {
            return;
        }
        java.util.Set<String> callerPermissions = resolveCallerPermissionsIfCustomAdmin(currentAdminUsername);
        if (callerPermissions == null) {
            return; // Super Admin -> toàn quyền
        }
        for (String perm : targetPermissions) {
            if (perm != null && !callerPermissions.contains(perm.trim().toUpperCase())) {
                throw new ForbiddenException("PRIVILEGE_ESCALATION_DETECTED",
                        "Bạn không thể cấp phát hoặc thao tác trên vai trò chứa quyền '" + perm + "' vượt quá phạm vi quyền hiện tại của bạn");
            }
        }
    }

    private java.util.Set<String> resolveCallerPermissionsIfCustomAdmin(String currentAdminUsername) {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            boolean hasCustomRole = auth.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .anyMatch(a -> a != null && a.startsWith("CUSTOM_ROLE_"));
            if (hasCustomRole) {
                return auth.getAuthorities().stream()
                        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                        .filter(a -> a != null && a.startsWith("PERM_"))
                        .map(a -> a.substring(5))
                        .collect(java.util.stream.Collectors.toSet());
            }
            boolean hasBaseAdmin = auth.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);
            if (hasBaseAdmin) {
                return null;
            }
        }
        if (currentAdminUsername != null && !currentAdminUsername.isBlank()) {
            Optional<User> callerOpt = userRepository.findByUsername(currentAdminUsername);
            if (callerOpt.isPresent()) {
                if (callerOpt.get().isSuperAdmin()) {
                    return null;
                }
                String callerRoleCode = callerOpt.get().getEffectiveRoleCode();
                return systemRoleRepository.findByCode(callerRoleCode)
                        .map(sr -> new java.util.HashSet<>(sr.getPermissions()))
                        .orElseGet(java.util.HashSet::new);
            }
            if ("admin".equalsIgnoreCase(currentAdminUsername.trim())) {
                return null;
            }
        }
        return java.util.Collections.emptySet(); // Fail-Closed
    }

    private SystemRoleResponse toResponse(SystemRole role) {
        long userCount = userRepository.countByRoleCode(role.getCode());
        return SystemRoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .category(role.getCategory())
                .permissions(role.getPermissions())
                .systemDefault(role.isSystemDefault())
                .userCount(userCount)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
