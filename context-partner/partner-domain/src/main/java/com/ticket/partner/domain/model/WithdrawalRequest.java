package com.ticket.partner.domain.model;

import com.ticket.shared.domain.BaseEntity;
import com.ticket.shared.exception.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequest extends BaseEntity {
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
    private String idempotencyKey;
    private LocalDateTime createdAt;

    public void validate() {
        if (amount == null || amount.compareTo(BigDecimal.valueOf(100000)) < 0) {
            throw new BusinessException("INVALID_WITHDRAWAL_AMOUNT", "Minimum withdrawal amount is 100,000 VND");
        }
        if (bankName == null || bankName.trim().isEmpty()) {
            throw new BusinessException("INVALID_BANK_NAME", "Bank name cannot be blank");
        }
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new BusinessException("INVALID_ACCOUNT_NUMBER", "Account number cannot be blank");
        }
        if (accountHolderName == null || accountHolderName.trim().isEmpty()) {
            throw new BusinessException("INVALID_ACCOUNT_HOLDER", "Account holder name cannot be blank");
        }
    }

    public void approve(Long adminId, String note) {
        if (this.status != WithdrawalStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Only PENDING withdrawal requests can be approved");
        }
        this.status = WithdrawalStatus.APPROVED;
        this.processedByAdminId = adminId;
        this.adminNote = note;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(Long adminId, String reason) {
        if (this.status != WithdrawalStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Only PENDING withdrawal requests can be rejected");
        }
        this.status = WithdrawalStatus.REJECTED;
        this.processedByAdminId = adminId;
        this.adminNote = reason;
        this.processedAt = LocalDateTime.now();
    }
}
