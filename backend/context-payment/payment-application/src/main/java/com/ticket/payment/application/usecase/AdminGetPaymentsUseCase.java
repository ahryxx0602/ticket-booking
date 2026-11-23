package com.ticket.payment.application.usecase;

import com.ticket.payment.application.dto.PaymentDetailResponse;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.shared.dto.PageResponse;

public interface AdminGetPaymentsUseCase {
    PageResponse<PaymentDetailResponse> execute(PaymentStatus status, PaymentMethod paymentMethod, String paymentReference, Boolean reconciliationRequired, int page, int size);

    default PageResponse<PaymentDetailResponse> execute(PaymentStatus status, PaymentMethod paymentMethod, String paymentReference, int page, int size) {
        return execute(status, paymentMethod, paymentReference, null, page, size);
    }
}
