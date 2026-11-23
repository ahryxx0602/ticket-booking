package com.ticket.notification.domain.model;

public enum DeviceType {
    WEB,
    ANDROID,
    IOS;

    public static DeviceType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return WEB;
        }
        try {
            return DeviceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return WEB;
        }
    }
}
