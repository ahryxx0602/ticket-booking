package com.ticket.payment.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class PaymentRevenueReport {
    private BigDecimal grossRevenue;
    private BigDecimal totalRefunded;
    private BigDecimal netRevenue;
    private long totalTransactions;
    private long successCount;
    private long failedCount;
    private long refundedCount;
    private long pendingCount;
    private long reconciliationCount;
    private Map<PaymentMethod, BigDecimal> revenueByMethod;
    private java.util.List<Payment> reconciliationPayments;
}
