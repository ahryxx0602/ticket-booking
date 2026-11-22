package com.ticket.booking.presentation.controller;

import com.ticket.booking.application.dto.AdminRefundCommand;
import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.RefundResponse;
import com.ticket.booking.application.usecase.AdminGetBookingsUseCase;
import com.ticket.booking.application.usecase.AdminRefundBookingUseCase;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.shared.dto.ApiResponse;
import com.ticket.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final AdminGetBookingsUseCase adminGetBookingsUseCase;
    private final AdminRefundBookingUseCase adminRefundBookingUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long tripId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<BookingResponse> response = adminGetBookingsUseCase.execute(status, tripId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Admin bookings retrieved successfully", response));
    }

    @PostMapping("/{bookingReference}/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> refundBooking(
            @PathVariable String bookingReference,
            @Valid @RequestBody AdminRefundCommand command) {
        RefundResponse response = adminRefundBookingUseCase.execute(bookingReference, command);
        return ResponseEntity.ok(ApiResponse.ok("Booking refunded successfully", response));
    }
}
