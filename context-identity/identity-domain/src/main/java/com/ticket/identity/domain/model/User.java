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
    private UserStatus status;

    public void lock() {
        this.status = UserStatus.LOCKED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void changeRole(UserRole newRole) {
        if (newRole != null) {
            this.role = newRole;
        }
    }

    public void changeRole(String newRole) {
        this.role = UserRole.fromString(newRole);
    }

    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void updateProfile(String fullName, String email) {
        if (fullName != null && !fullName.trim().isEmpty()) {
            this.fullName = fullName.trim();
        }
        if (email != null && !email.trim().isEmpty()) {
            this.email = email.trim();
        }
    }

    public boolean isLocked() {
        return UserStatus.LOCKED.equals(this.status);
    }
}
