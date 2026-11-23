package com.ticket.booking.infrastructure.scheduler;

import com.ticket.booking.application.usecase.ExpireHeldBookingsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingExpirationSchedulerTest {

    @Mock
    private ExpireHeldBookingsUseCase expireHeldBookingsUseCase;

    @InjectMocks
    private BookingExpirationScheduler scheduler;

    @Test
    void scanAndExpireBookings_shouldInvokeUseCase() {
        when(expireHeldBookingsUseCase.execute(any(LocalDateTime.class))).thenReturn(3);

        scheduler.scanAndExpireBookings();

        verify(expireHeldBookingsUseCase, times(1)).execute(any(LocalDateTime.class));
    }

    @Test
    void scanAndExpireBookings_whenExceptionThrown_shouldNotPropagate() {
        when(expireHeldBookingsUseCase.execute(any(LocalDateTime.class))).thenThrow(new RuntimeException("Database error"));

        // Should handle exception gracefully without throwing
        scheduler.scanAndExpireBookings();

        verify(expireHeldBookingsUseCase, times(1)).execute(any(LocalDateTime.class));
    }
}
