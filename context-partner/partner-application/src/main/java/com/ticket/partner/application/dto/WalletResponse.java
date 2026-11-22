package com.ticket.partner.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Long id;
    private Long operatorId;
    private BigDecimal availableBalance;
    private BigDecimal pendingSettlementBalance;
    private BigDecimal lockedBalance;
    private BigDecimal debtBalance;
    private String currency;
}
