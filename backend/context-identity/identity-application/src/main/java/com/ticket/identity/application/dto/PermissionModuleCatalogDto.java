package com.ticket.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionModuleCatalogDto {
    private String moduleCode;
    private String moduleName;
    private String routePath;
    private String viewPermissionCode;
    private List<PermissionItemDto> permissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionItemDto {
        private String code;
        private String name;
        private String httpMethod;
        private String apiPattern;
        private String description;
        private boolean viewPermission;
    }
}
