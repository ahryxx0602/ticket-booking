package com.ticket.payment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReportResponse {
    private BigDecimal grossRevenue;
    private BigDecimal totalRefunded;
    private BigDecimal netRevenue;
    private long totalTransactions;
    private long successCount;
    private long failedCount;
    private long refundedCount;
    private long pendingCount;
    private long reconciliationCount;
    private Map<String, BigDecimal> revenueByMethod;
    private java.util.List<PaymentDetailResponse> reconciliationTransactions;
}
