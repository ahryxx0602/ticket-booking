package com.ticket.payment.application.usecase;

import com.ticket.payment.application.dto.CustomerPaymentStatusResponse;

public interface GetCustomerPaymentStatusUseCase {
    CustomerPaymentStatusResponse execute(String bookingReference, String authenticatedUsername);
}
