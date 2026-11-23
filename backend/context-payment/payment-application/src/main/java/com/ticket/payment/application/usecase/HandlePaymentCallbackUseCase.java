package com.ticket.payment.application.usecase;

import com.ticket.payment.application.dto.VnPayCallbackDto;
import com.ticket.payment.application.dto.VnPayIpnResponse;

public interface HandlePaymentCallbackUseCase {
    VnPayIpnResponse execute(VnPayCallbackDto callbackDto);
}
