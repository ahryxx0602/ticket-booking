package com.ticket.booking.infrastructure.service;

import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.usecase.AdminGetBookingsUseCase;
import com.ticket.booking.application.usecase.GetBookingDetailUseCase;
import com.ticket.booking.application.usecase.GetMyBookingsUseCase;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.port.IdentityUserPort;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingQueryServiceImpl implements GetMyBookingsUseCase, GetBookingDetailUseCase, AdminGetBookingsUseCase {

    private final BookingRepository bookingRepository;
    private final IdentityUserPort identityUserPort;

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public PageResponse<BookingResponse> execute(String authenticatedUsername, int page, int size) {
        IdentityUserPort.AuthenticatedUserInfo user = requireActiveUser(authenticatedUsername);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        PageResponse<Booking> result = bookingRepository.findByUserId(user.userId(), safePage, safeSize);
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
