package com.ticket.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemRoleRequest {

    @NotBlank(message = "Mã Role không được để trống")
    private String code;

    @NotBlank(message = "Tên hiển thị của Role không được để trống")
    private String name;

    private String description;

    private String category;

    private List<String> permissions;
}
