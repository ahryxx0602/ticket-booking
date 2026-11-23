package com.ticket.payment.application.usecase;

import com.ticket.payment.application.dto.CreatePaymentCommand;
import com.ticket.payment.application.dto.PaymentInitResponse;

public interface CreatePaymentUseCase {
    PaymentInitResponse execute(CreatePaymentCommand command, String username, String clientIp);
}
