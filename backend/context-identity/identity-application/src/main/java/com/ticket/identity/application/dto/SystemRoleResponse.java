package com.ticket.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemRoleResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String category;
    private List<String> permissions;
    private boolean systemDefault;
    private long userCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
