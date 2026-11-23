package com.ticket.booking.infrastructure.scheduler;

import com.ticket.booking.application.usecase.ExpireHeldBookingsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final ExpireHeldBookingsUseCase expireHeldBookingsUseCase;

    @Scheduled(fixedDelayString = "${app.booking.scheduler.delay-ms:30000}")
    public void scanAndExpireBookings() {
        try {
            int expiredCount = expireHeldBookingsUseCase.execute(LocalDateTime.now());
            if (expiredCount > 0) {
                log.info("[BOOKING SCHEDULER] Expired {} held booking(s) and released seat locks", expiredCount);
            }
        } catch (Exception e) {
            log.error("[BOOKING SCHEDULER] Error during scan and expire: {}", e.getMessage(), e);
        }
    }
}
