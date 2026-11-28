package com.ticket.booking.infrastructure.service;

import com.ticket.booking.application.dto.BookingItemDto;
import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingItem;

import java.util.Collections;
import java.util.List;

final class BookingMapper {

    private BookingMapper() {
    }

    static BookingResponse toResponse(Booking booking) {
        List<BookingItem> items = booking.getItems() != null ? booking.getItems() : Collections.emptyList();
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUserId())
                .tripId(booking.getTripId())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .expiresAt(booking.getExpiresAt())
                .createdAt(booking.getCreatedAt())
                .clientRequestId(booking.getClientRequestId())
                .seatNumbers(items.stream().map(BookingItem::getSeatNumber).toList())
                .items(items.stream().map(i -> BookingItemDto.builder()
                        .seatId(i.getSeatId())
                        .seatNumber(i.getSeatNumber())
                        .price(i.getPrice())
                        .build()).toList())
                .build();
    }
}
