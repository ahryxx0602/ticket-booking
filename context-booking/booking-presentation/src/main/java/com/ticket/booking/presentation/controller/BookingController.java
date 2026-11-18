package com.ticket.booking.presentation.controller;

import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.HoldSeatCommand;
import com.ticket.booking.application.usecase.CancelBookingUseCase;
import com.ticket.booking.application.usecase.HoldSeatUseCase;
import com.ticket.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final HoldSeatUseCase holdSeatUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;

    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<BookingResponse>> holdSeats(@Valid @RequestBody HoldSeatCommand command) {
        BookingResponse response = holdSeatUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.ok("Seats held successfully for 10 minutes", response));
    }

    @PostMapping("/{bookingReference}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(@PathVariable String bookingReference) {
        cancelBookingUseCase.execute(bookingReference);
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled successfully", null));
    }
}
