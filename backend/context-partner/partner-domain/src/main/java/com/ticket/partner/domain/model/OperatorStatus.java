package com.ticket.partner.domain.model;

public enum OperatorStatus {
    PENDING_APPROVAL,
    ACTIVE,
    SUSPENDED,
    REJECTED;

    public boolean canOperate() {
        return this == ACTIVE;
    }
}
