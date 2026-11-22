package com.ticket.partner.application.dto;

import com.ticket.partner.domain.model.OperatorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorResponse {
    private Long id;
    private Long userId;
    private String businessName;
    private String brandName;
    private String taxCode;
    private String contactPhone;
    private String contactEmail;
    private String logoUrl;
    private String description;
    private BigDecimal commissionRate;
    private OperatorStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
