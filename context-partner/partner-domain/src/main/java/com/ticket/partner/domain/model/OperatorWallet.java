package com.ticket.partner.domain.model;

import com.ticket.shared.domain.BaseEntity;
import com.ticket.shared.exception.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorWallet extends BaseEntity {
    private Long id;
    private Long operatorId;
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal pendingSettlementBalance = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal debtBalance = BigDecimal.ZERO;
    @Builder.Default
    private String currency = "VND";
    private Long version;

    public void creditPending(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal normalized = amount.setScale(0, RoundingMode.HALF_UP);
        this.pendingSettlementBalance = this.pendingSettlementBalance.add(normalized);
    }

    public void settleToAvailable(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal normalized = amount.setScale(0, RoundingMode.HALF_UP);
        if (this.pendingSettlementBalance.compareTo(normalized) < 0) {
            normalized = this.pendingSettlementBalance;
        }
        this.pendingSettlementBalance = this.pendingSettlementBalance.subtract(normalized);

        // Nếu có nợ, tự động cấn trừ nợ trước
        if (this.debtBalance.compareTo(BigDecimal.ZERO) > 0) {
            if (normalized.compareTo(this.debtBalance) >= 0) {
                normalized = normalized.subtract(this.debtBalance);
                this.debtBalance = BigDecimal.ZERO;
            } else {
                this.debtBalance = this.debtBalance.subtract(normalized);
                normalized = BigDecimal.ZERO;
            }
        }
        this.availableBalance = this.availableBalance.add(normalized);
    }

    public void lockForWithdrawal(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Withdrawal amount must be greater than zero");
        }
        if (this.debtBalance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("DEBT_PENDING", "Cannot withdraw while having outstanding debt: " + this.debtBalance + " VND");
        }
        BigDecimal normalized = amount.setScale(0, RoundingMode.HALF_UP);
        if (this.availableBalance.compareTo(normalized) < 0) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "Available balance is not enough for withdrawal: " + this.availableBalance);
        }
        this.availableBalance = this.availableBalance.subtract(normalized);
        this.lockedBalance = this.lockedBalance.add(normalized);
    }

    public void releaseLocked(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal normalized = amount.setScale(0, RoundingMode.HALF_UP);
        if (this.lockedBalance.compareTo(normalized) < 0) {
            normalized = this.lockedBalance;
        }
        this.lockedBalance = this.lockedBalance.subtract(normalized);
        this.availableBalance = this.availableBalance.add(normalized);
    }

    public void debitLocked(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal normalized = amount.setScale(0, RoundingMode.HALF_UP);
        if (this.lockedBalance.compareTo(normalized) < 0) {
            normalized = this.lockedBalance;
        }
        this.lockedBalance = this.lockedBalance.subtract(normalized);
    }

    public void refundDeduction(BigDecimal amount, boolean isPending) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal normalized = amount.setScale(0, RoundingMode.HALF_UP);
        if (isPending) {
            if (this.pendingSettlementBalance.compareTo(normalized) >= 0) {
                this.pendingSettlementBalance = this.pendingSettlementBalance.subtract(normalized);
            } else {
                BigDecimal remaining = normalized.subtract(this.pendingSettlementBalance);
                this.pendingSettlementBalance = BigDecimal.ZERO;
                deductAvailableOrDebt(remaining);
            }
        } else {
            deductAvailableOrDebt(normalized);
        }
    }

    private void deductAvailableOrDebt(BigDecimal amount) {
        if (this.availableBalance.compareTo(amount) >= 0) {
            this.availableBalance = this.availableBalance.subtract(amount);
        } else {
            BigDecimal deficit = amount.subtract(this.availableBalance);
            this.availableBalance = BigDecimal.ZERO;
            this.debtBalance = this.debtBalance.add(deficit);
        }
    }
}
