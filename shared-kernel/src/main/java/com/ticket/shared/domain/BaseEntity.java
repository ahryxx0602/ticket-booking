package com.ticket.shared.domain;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public abstract class BaseEntity {
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
