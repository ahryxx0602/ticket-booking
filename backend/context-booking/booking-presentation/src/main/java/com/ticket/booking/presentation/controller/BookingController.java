package com.ticket.booking.presentation.controller;

import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.HoldSeatCommand;
import com.ticket.booking.application.dto.SeatAvailabilityResponse;
import com.ticket.booking.application.dto.TicketResponse;
import com.ticket.booking.application.usecase.CancelBookingUseCase;
import com.ticket.booking.application.usecase.GetBookingDetailUseCase;
import com.ticket.booking.application.usecase.GetMyBookingsUseCase;
import com.ticket.booking.application.usecase.GetSeatAvailabilityUseCase;
import com.ticket.booking.application.usecase.GetTicketUseCase;
import com.ticket.booking.application.usecase.HoldSeatUseCase;
import com.ticket.shared.dto.ApiResponse;
import com.ticket.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final HoldSeatUseCase holdSeatUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final GetMyBookingsUseCase getMyBookingsUseCase;
    private final GetBookingDetailUseCase getBookingDetailUseCase;
    private final GetSeatAvailabilityUseCase getSeatAvailabilityUseCase;
    private final GetTicketUseCase getTicketUseCase;

    @GetMapping("/trips/{tripId}/availability")
    public ResponseEntity<ApiResponse<SeatAvailabilityResponse>> getSeatAvailability(
            @PathVariable Long tripId) {
        SeatAvailabilityResponse response = getSeatAvailabilityUseCase.execute(tripId);
        return ResponseEntity.ok(ApiResponse.ok("Seat availability retrieved successfully", response));
    }

    @PostMapping("/hold")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> holdSeats(
            @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody HoldSeatCommand command,
            Principal principal) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String trimmedKey = idempotencyKey.trim();
            if (trimmedKey.length() > 64 || !trimmedKey.matches("^[0-9a-zA-Z_-]{1,64}$")) {
                throw new com.ticket.shared.exception.BusinessException(
                        "INVALID_IDEMPOTENCY_KEY",
                        "Idempotency-Key header must be an alphanumeric/hyphen string up to 64 characters"
                );
            }
            command.setClientRequestId(trimmedKey);
        }
        BookingResponse response = holdSeatUseCase.execute(command, requireUsername(principal));
        return ResponseEntity.ok(ApiResponse.ok("Seats held successfully for 10 minutes", response));
    }

    @PostMapping("/{bookingReference}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @PathVariable String bookingReference,
            Principal principal) {
        cancelBookingUseCase.execute(bookingReference, requireUsername(principal));
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled successfully", null));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            Principal principal) {
        PageResponse<BookingResponse> response = getMyBookingsUseCase.execute(requireUsername(principal), page, size, status);
        return ResponseEntity.ok(ApiResponse.ok("Bookings retrieved successfully", response));
    }

    @GetMapping("/{bookingReference}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingDetail(
            @PathVariable String bookingReference,
            Principal principal) {
        BookingResponse response = getBookingDetailUseCase.execute(bookingReference, requireUsername(principal));
        return ResponseEntity.ok(ApiResponse.ok("Booking details retrieved successfully", response));
    }

    @GetMapping("/{bookingReference}/ticket")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(
            @PathVariable String bookingReference,
            Principal principal) {
        TicketResponse response = getTicketUseCase.getTicket(bookingReference, requireUsername(principal));
        return ResponseEntity.ok(ApiResponse.ok("Electronic ticket retrieved successfully", response));
    }

    private String requireUsername(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }
        return principal.getName();
    }
}
