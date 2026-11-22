package com.ticket.booking.domain.repository;

import java.time.Duration;
import java.util.List;

public interface SeatLockRepository {
    boolean acquireLocks(Long tripId, List<String> seatNumbers, String ownerToken, Duration ttl);
    void releaseOwnedLocks(Long tripId, List<String> seatNumbers, String ownerToken);
    boolean isSeatLocked(Long tripId, String seatNumber);
}
