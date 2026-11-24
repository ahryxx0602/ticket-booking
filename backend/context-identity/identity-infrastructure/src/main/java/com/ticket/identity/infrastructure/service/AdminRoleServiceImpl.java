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

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            existing.setCategory(request.getCategory());
        }
        existing.setPermissions(request.getPermissions());
        existing.validateAndNormalize();

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
