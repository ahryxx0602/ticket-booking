package com.ticket.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeUserRoleRequest {
    @NotBlank(message = "Role is required")
    private String role;
}
