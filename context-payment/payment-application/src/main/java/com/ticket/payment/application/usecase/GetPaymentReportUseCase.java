package com.ticket.payment.application.usecase;

import com.ticket.payment.application.dto.PaymentReportResponse;

import java.time.LocalDate;

public interface GetPaymentReportUseCase {
    PaymentReportResponse execute(LocalDate startDate, LocalDate endDate);
}
