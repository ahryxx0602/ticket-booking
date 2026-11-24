package com.ticket.payment.application.usecase;

import com.ticket.payment.application.dto.PaymentDetailResponse;

public interface AdminReconcilePaymentUseCase {
    PaymentDetailResponse execute(Long paymentId, String resolutionNote);
}
