package com.ticket.booking.infrastructure.lock;

import com.ticket.booking.domain.repository.SeatLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisSeatLockRepository implements SeatLockRepository {

    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "lock:seat:";

    @Override
    public boolean acquireLocks(Long tripId, List<String> seatNumbers, Long userId, Duration ttl) {
        List<RLock> acquiredLocks = new ArrayList<>();
        try {
            for (String seatNumber : seatNumbers) {
                String lockKey = String.format("%s%d:%s", LOCK_PREFIX, tripId, seatNumber);
                RLock lock = redissonClient.getLock(lockKey);
                // Attempt to acquire lock with 2s wait time and given TTL lease time
                boolean acquired = lock.tryLock(2, ttl.getSeconds(), TimeUnit.SECONDS);
                if (!acquired) {
                    // Rollback acquired locks in this attempt
                    acquiredLocks.forEach(l -> {
                        try { l.unlock(); } catch (Exception ignored) {}
                    });
                    return false;
                }
                acquiredLocks.add(lock);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            acquiredLocks.forEach(l -> {
                try { l.unlock(); } catch (Exception ignored) {}
            });
            return false;
        }
    }

    @Override
    public void releaseLocks(Long tripId, List<String> seatNumbers) {
        for (String seatNumber : seatNumbers) {
            String lockKey = String.format("%s%d:%s", LOCK_PREFIX, tripId, seatNumber);
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean isSeatLocked(Long tripId, String seatNumber) {
        String lockKey = String.format("%s%d:%s", LOCK_PREFIX, tripId, seatNumber);
        return redissonClient.getLock(lockKey).isLocked();
    }
}
