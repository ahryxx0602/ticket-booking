package com.ticket.booking.domain.repository;

import java.time.Duration;
import java.util.List;

public interface SeatLockRepository {
    boolean acquireLocks(Long tripId, List<String> seatNumbers, Long userId, Duration ttl);
    void releaseLocks(Long tripId, List<String> seatNumbers);
    boolean isSeatLocked(Long tripId, String seatNumber);
}
