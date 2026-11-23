package com.ticket.partner.domain.model;

public enum BannerPosition {
    HOMEPAGE_HERO,
    HOMEPAGE_BANNER,
    SEARCH_SIDEBAR;

    public static BannerPosition fromString(String value) {
        if (value == null) return HOMEPAGE_HERO;
        try {
            return BannerPosition.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return HOMEPAGE_HERO;
        }
    }
}
