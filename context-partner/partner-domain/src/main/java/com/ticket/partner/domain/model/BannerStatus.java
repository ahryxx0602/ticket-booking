package com.ticket.partner.domain.model;

public enum BannerStatus {
    PENDING_APPROVAL,
    ACTIVE,
    REJECTED,
    EXPIRED;

    public boolean isDisplayable() {
        return this == ACTIVE;
    }
}
