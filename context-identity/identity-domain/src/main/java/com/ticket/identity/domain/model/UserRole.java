package com.ticket.identity.domain.model;

public enum UserRole {
    ROLE_CUSTOMER,
    ROLE_ADMIN,
    ROLE_OPERATOR;

    public static UserRole fromString(String role) {
        if (role == null || role.trim().isEmpty()) {
            return ROLE_CUSTOMER;
        }
        String clean = role.trim().toUpperCase();
        if (!clean.startsWith("ROLE_")) {
            clean = "ROLE_" + clean;
        }
        try {
            return UserRole.valueOf(clean);
        } catch (IllegalArgumentException e) {
            return ROLE_CUSTOMER;
        }
    }
}
