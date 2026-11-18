package com.ticket.identity.application.dto;

import com.ticket.identity.domain.model.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {
    @NotNull(message = "Status is required")
    private UserStatus status;
}
