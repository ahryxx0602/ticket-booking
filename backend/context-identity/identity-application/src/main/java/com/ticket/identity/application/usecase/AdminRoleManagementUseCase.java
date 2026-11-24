package com.ticket.identity.application.usecase;

import com.ticket.identity.application.dto.PermissionModuleCatalogDto;
import com.ticket.identity.application.dto.SystemRoleRequest;
import com.ticket.identity.application.dto.SystemRoleResponse;

import java.util.List;

public interface AdminRoleManagementUseCase {
    List<PermissionModuleCatalogDto> getPermissionCatalog();
    List<SystemRoleResponse> getAllRoles();
    SystemRoleResponse createRole(SystemRoleRequest request, String currentAdminUsername);
    SystemRoleResponse updateRole(Long id, SystemRoleRequest request, String currentAdminUsername);
    void deleteRole(Long id, String currentAdminUsername);
}
