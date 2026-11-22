package com.ticket.partner.application.dto;

import com.ticket.partner.domain.model.WithdrawalStatus;
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
public class WithdrawalResponse {
    private Long id;
    private Long operatorId;
    private BigDecimal amount;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private WithdrawalStatus status;
    private String adminNote;
    private Long processedByAdminId;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
