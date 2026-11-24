package com.ticket.identity.domain.model;

import com.ticket.shared.domain.BaseEntity;
import com.ticket.shared.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Thực thể Domain đại diện cho một Vai trò Hệ thống (System Role) trong RBAC động.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemRole extends BaseEntity {

    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("^ROLE_[A-Z0-9_]{2,45}$");

    private Long id;
    private String code;
    private String name;
    private String description;
    @Builder.Default
    private String category = "SYSTEM"; // CUSTOMER, OPERATOR, SYSTEM
    @Builder.Default
    private List<String> permissions = new ArrayList<>();
    @Builder.Default
    private boolean systemDefault = false;

    public static String normalizeCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new BusinessException("INVALID_ROLE_CODE", "Mã Role không được để trống");
        }
        String normalized = rawCode.trim().toUpperCase().replaceAll("[\\s-]+", "_");
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        if (!ROLE_CODE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("INVALID_ROLE_CODE",
                    "Mã Role không hợp lệ (chỉ chấp nhận chữ cái in hoa, chữ số và dấu gạch dưới, ví dụ: ROLE_ACCOUNTANT)");
        }
        return normalized;
    }

    public void validateAndNormalize() {
        this.code = normalizeCode(this.code);
        if (this.name == null || this.name.isBlank()) {
            throw new BusinessException("INVALID_ROLE_NAME", "Tên hiển thị của Role không được để trống");
        }
        this.name = this.name.trim();
        // FND-006: Mọi Custom Role (!systemDefault) bắt buộc thuộc category = SYSTEM (Phía hệ thống Admin)
        if (!this.systemDefault) {
            this.category = "SYSTEM";
        } else if (this.category == null || this.category.isBlank()) {
            this.category = "SYSTEM";
        } else {
            this.category = this.category.trim().toUpperCase();
        }
        this.permissions = AdminPermissionCatalog.sanitizePermissions(this.permissions);
    }

    public UserRole toBaseUserRole() {
        if ("ROLE_CUSTOMER".equalsIgnoreCase(this.code)) {
            return UserRole.ROLE_CUSTOMER;
        }
        if ("ROLE_OPERATOR".equalsIgnoreCase(this.code)) {
            return UserRole.ROLE_OPERATOR;
        }
        return UserRole.ROLE_ADMIN;
    }
}
