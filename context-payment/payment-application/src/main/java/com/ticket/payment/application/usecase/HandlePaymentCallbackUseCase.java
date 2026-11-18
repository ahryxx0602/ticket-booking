package com.ticket.payment.application.usecase;

import com.ticket.payment.application.dto.VnPayCallbackDto;

public interface HandlePaymentCallbackUseCase {
    boolean execute(VnPayCallbackDto callbackDto);
}
