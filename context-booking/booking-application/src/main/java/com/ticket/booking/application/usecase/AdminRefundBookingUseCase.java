package com.ticket.booking.application.usecase;

import com.ticket.booking.application.dto.AdminRefundCommand;
import com.ticket.booking.application.dto.RefundResponse;

public interface AdminRefundBookingUseCase {
    RefundResponse execute(String bookingReference, AdminRefundCommand command);
}
