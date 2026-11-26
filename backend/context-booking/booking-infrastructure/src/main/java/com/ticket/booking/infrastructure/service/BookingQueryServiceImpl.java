package com.ticket.booking.infrastructure.service;

import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.SeatAvailabilityItemResponse;
import com.ticket.booking.application.dto.SeatAvailabilityResponse;
import com.ticket.booking.application.dto.TicketResponse;
import com.ticket.booking.application.usecase.AdminGetBookingsUseCase;
import com.ticket.booking.application.usecase.GetBookingDetailUseCase;
import com.ticket.booking.application.usecase.GetMyBookingsUseCase;
import com.ticket.booking.application.usecase.GetSeatAvailabilityUseCase;
import com.ticket.booking.application.usecase.GetTicketUseCase;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.port.BookingTicketQrPort;
import com.ticket.booking.domain.port.CatalogTripPort;
import com.ticket.booking.domain.port.IdentityUserPort;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.booking.domain.repository.SeatLockRepository;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingQueryServiceImpl implements GetMyBookingsUseCase, GetBookingDetailUseCase, AdminGetBookingsUseCase, GetSeatAvailabilityUseCase, GetTicketUseCase {

    private final BookingRepository bookingRepository;
    private final IdentityUserPort identityUserPort;
    private final CatalogTripPort catalogTripPort;
    private final SeatLockRepository seatLockRepository;
    private final BookingTicketQrPort bookingTicketQrPort;

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public PageResponse<BookingResponse> execute(String authenticatedUsername, int page, int size) {
        return execute(authenticatedUsername, page, size, null);
    }

    @Override
    public PageResponse<BookingResponse> execute(String authenticatedUsername, int page, int size, String status) {
        IdentityUserPort.AuthenticatedUserInfo user = requireActiveUser(authenticatedUsername);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        PageResponse<Booking> result;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            try {
                BookingStatus bookingStatus = BookingStatus.valueOf(status.trim().toUpperCase());
                result = bookingRepository.findByUserIdAndStatus(user.userId(), bookingStatus, safePage, safeSize);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("INVALID_BOOKING_STATUS", "Trạng thái đặt vé không hợp lệ: " + status);
            }
        } else {
            result = bookingRepository.findByUserId(user.userId(), safePage, safeSize);
        }
        return mapPage(result);
    }

    @Override
    public BookingResponse execute(String bookingReference, String authenticatedUsername) {
        IdentityUserPort.AuthenticatedUserInfo user = requireActiveUser(authenticatedUsername);

        Booking booking = bookingRepository.findByReference(bookingReference)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingReference));

        booking.ensureOwnedBy(user.userId());
        return BookingMapper.toResponse(booking);
    }

    @Override
    public TicketResponse getTicket(String bookingReference, String authenticatedUsername) {
        IdentityUserPort.AuthenticatedUserInfo user = requireActiveUser(authenticatedUsername);

        Booking booking = bookingRepository.findByReference(bookingReference)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingReference));

        booking.ensureOwnedBy(user.userId());

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException(
                    "BOOKING_NOT_ELIGIBLE_FOR_TICKET",
                    "Chỉ có thể xuất vé điện tử cho đơn đặt chỗ đã xác nhận thanh toán (Trạng thái hiện tại: " + booking.getStatus() + ")"
            );
        }

        String qrPayload = bookingTicketQrPort.buildQrPayload(booking.getBookingReference(), booking.getTripId());
        String qrImageBase64 = bookingTicketQrPort.generateQrCodeBase64(qrPayload);

        return TicketResponse.builder()
                .bookingReference(booking.getBookingReference())
                .tripId(booking.getTripId())
                .seatNumbers(booking.getSeatNumbers())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .qrPayload(qrPayload)
                .qrImageBase64(qrImageBase64)
                .issuedAt(LocalDateTime.now())
                .build();
    }

    private IdentityUserPort.AuthenticatedUserInfo requireActiveUser(String authenticatedUsername) {
        IdentityUserPort.AuthenticatedUserInfo user = identityUserPort.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new EntityNotFoundException("User", authenticatedUsername));
        if (user.locked()) {
            throw new BusinessException("ACCOUNT_LOCKED", "Your account has been locked by administrator");
        }
        return user;
    }

    @Override
    public PageResponse<BookingResponse> execute(BookingStatus status, Long tripId, Long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        PageResponse<Booking> result = bookingRepository.findAllWithFilters(status, tripId, userId, safePage, safeSize);
        return mapPage(result);
    }

    @Override
    public SeatAvailabilityResponse execute(Long tripId) {
        if (tripId == null) {
            throw new IllegalArgumentException("Trip ID must not be null");
        }

        CatalogTripPort.TripSnapshot tripSnapshot = catalogTripPort.findTripWithSeats(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Trip", tripId));

        List<String> catalogSeatNumbers = tripSnapshot.seatsByNumber().keySet().stream()
                .sorted()
                .toList();

        if (catalogSeatNumbers.isEmpty()) {
            return SeatAvailabilityResponse.builder()
                    .tripId(tripId)
                    .serverTime(OffsetDateTime.now(ZoneOffset.ofHours(7)))
                    .seats(Collections.emptyList())
                    .build();
        }

        Set<String> confirmedSeats = bookingRepository.findConfirmedSeatNumbers(tripId);
        Set<String> activeHeldDbSeats = bookingRepository.findActiveHeldSeatNumbers(tripId, LocalDateTime.now());
        Set<String> redisLockedSeats = seatLockRepository.findLockedSeats(tripId, catalogSeatNumbers);

        List<SeatAvailabilityItemResponse> items = catalogSeatNumbers.stream()
                .map(seatNumber -> {
                    String status;
                    if (confirmedSeats.contains(seatNumber)) {
                        status = "SOLD";
                    } else if (activeHeldDbSeats.contains(seatNumber) || redisLockedSeats.contains(seatNumber)) {
                        status = "HELD";
                    } else {
                        status = "AVAILABLE";
                    }
                    return new SeatAvailabilityItemResponse(seatNumber, status);
                })
                .toList();

        return SeatAvailabilityResponse.builder()
                .tripId(tripId)
                .serverTime(OffsetDateTime.now(ZoneOffset.ofHours(7)))
                .seats(items)
                .build();
    }

    private PageResponse<BookingResponse> mapPage(PageResponse<Booking> source) {
        return PageResponse.<BookingResponse>builder()
                .items(source.getItems().stream().map(BookingMapper::toResponse).toList())
                .page(source.getPage())
                .size(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .hasNext(source.isHasNext())
                .hasPrevious(source.isHasPrevious())
                .build();
    }
}
