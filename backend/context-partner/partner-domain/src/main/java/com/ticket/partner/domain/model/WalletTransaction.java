package com.ticket.partner.domain.model;

import com.ticket.shared.domain.BaseEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction extends BaseEntity {
    private Long id;
    private Long walletId;
    private Long operatorId;
    private Long bookingId;
    private Long tripId;
    private String paymentReference;
    private WalletTransactionType transactionType;
    private BigDecimal grossAmount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt;
}
