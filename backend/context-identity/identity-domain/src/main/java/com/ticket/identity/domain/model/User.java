package com.ticket.identity.domain.model;

import com.ticket.shared.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private UserRole role;
    private String customRoleCode;
    private UserStatus status;
    private Long tokenVersion;

    public long getEffectiveTokenVersion() {
        return this.tokenVersion != null ? this.tokenVersion : 0L;
    }

    public void incrementTokenVersion() {
        this.tokenVersion = getEffectiveTokenVersion() + 1L;
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
        incrementTokenVersion();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void changeRole(UserRole newRole) {
        if (newRole != null) {
            this.role = newRole;
            this.customRoleCode = newRole.name();
            incrementTokenVersion();
        }
    }

    public void changeRole(String newRole) {
        UserRole parsed = UserRole.fromString(newRole);
        this.role = parsed;
        this.customRoleCode = parsed.name();
        incrementTokenVersion();
    }

    public void assignSystemRole(UserRole baseRole, String customCode) {
        this.role = baseRole != null ? baseRole : UserRole.ROLE_CUSTOMER;
        this.customRoleCode = (customCode != null && !customCode.isBlank())
                ? customCode.trim().toUpperCase()
                : this.role.name();
        incrementTokenVersion();
    }

    public String getEffectiveRoleCode() {
        if (this.customRoleCode != null && !this.customRoleCode.isBlank()) {
            return this.customRoleCode.trim().toUpperCase();
        }
        return this.role != null ? this.role.name() : "ROLE_CUSTOMER";
    }

    public boolean isSuperAdmin() {
        return UserRole.ROLE_ADMIN.equals(this.role)
                && "ROLE_ADMIN".equalsIgnoreCase(getEffectiveRoleCode());
    }

    public String getCategory() {
        if (UserRole.ROLE_ADMIN.equals(this.role)) {
            return "SYSTEM";
        }
        if (UserRole.ROLE_OPERATOR.equals(this.role)) {
            return "OPERATOR";
        }
        return "CUSTOMER";
    }

    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        incrementTokenVersion();
    }

    public void updateProfile(String fullName, String email) {
        if (fullName != null && !fullName.trim().isEmpty()) {
            this.fullName = fullName.trim();
        }
        if (email != null && !email.trim().isEmpty()) {
            this.email = email.trim();
        }
    }

    public void updateProfile(String fullName, String email, String phone) {
        updateProfile(fullName, email);
        if (phone != null) {
            this.phone = phone.trim();
        }
    }

    public boolean isLocked() {
        return UserStatus.LOCKED.equals(this.status);
    }
}
