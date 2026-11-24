package com.ticket.identity.presentation.controller;

import com.ticket.identity.application.dto.PermissionModuleCatalogDto;
import com.ticket.identity.application.dto.SystemRoleRequest;
import com.ticket.identity.application.dto.SystemRoleResponse;
import com.ticket.identity.application.usecase.AdminRoleManagementUseCase;
import com.ticket.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final AdminRoleManagementUseCase roleManagementUseCase;

    @GetMapping("/permissions-catalog")
    public ApiResponse<List<PermissionModuleCatalogDto>> getPermissionsCatalog() {
        List<PermissionModuleCatalogDto> catalog = roleManagementUseCase.getPermissionCatalog();
        return ApiResponse.ok("Permissions catalog retrieved successfully", catalog);
    }

    @GetMapping
    public ApiResponse<List<SystemRoleResponse>> getAllRoles() {
        List<SystemRoleResponse> roles = roleManagementUseCase.getAllRoles();
        return ApiResponse.ok("System roles retrieved successfully", roles);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemRoleResponse>> createRole(
            @Valid @RequestBody SystemRoleRequest request,
            Principal principal) {
        String adminUsername = principal != null ? principal.getName() : "admin";
        SystemRoleResponse created = roleManagementUseCase.createRole(request, adminUsername);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("System role created successfully", created));
    }

    @PutMapping("/{id}")
    public ApiResponse<SystemRoleResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody SystemRoleRequest request,
            Principal principal) {
        String adminUsername = principal != null ? principal.getName() : "admin";
        SystemRoleResponse updated = roleManagementUseCase.updateRole(id, request, adminUsername);
        return ApiResponse.ok("System role updated successfully", updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRole(
            @PathVariable Long id,
            Principal principal) {
        String adminUsername = principal != null ? principal.getName() : "admin";
        roleManagementUseCase.deleteRole(id, adminUsername);
        return ApiResponse.ok("System role deleted successfully", null);
    }
}
