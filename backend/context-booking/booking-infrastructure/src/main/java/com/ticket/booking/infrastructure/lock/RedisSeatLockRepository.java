package com.ticket.booking.infrastructure.lock;

import com.ticket.booking.domain.repository.SeatLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisSeatLockRepository implements SeatLockRepository {

    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "lock:seat:";
    private static final long WAIT_TIME_MS = 2000L;
    private static final long RETRY_INTERVAL_MS = 50L;

    // Atomic CAS delete: only deletes the lock if the stored ownerToken matches ARGV[1]
    private static final String LUA_CAS_DELETE =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    @Override
    public boolean acquireLocks(Long tripId, List<String> seatNumbers, String ownerToken, Duration ttl) {
        if (seatNumbers == null || seatNumbers.isEmpty() || ownerToken == null || ownerToken.isBlank()) {
            return false;
        }
        List<String> sortedSeats = seatNumbers.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList();

        List<String> acquiredKeys = new ArrayList<>();
        try {
            for (String seatNumber : sortedSeats) {
                String lockKey = buildKey(tripId, seatNumber);
                if (!tryAcquireSingleLock(lockKey, ownerToken, ttl)) {
                    releaseKeysSafely(acquiredKeys, ownerToken);
                    return false;
                }
                acquiredKeys.add(lockKey);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            releaseKeysSafely(acquiredKeys, ownerToken);
            return false;
        } catch (RuntimeException ex) {
            log.warn("Redis error while acquiring seat locks for tripId={}: {}", tripId, ex.getMessage());
            releaseKeysSafely(acquiredKeys, ownerToken);
            throw ex;
        }
    }

    @Override
    public void releaseOwnedLocks(Long tripId, List<String> seatNumbers, String ownerToken) {
        if (seatNumbers == null || seatNumbers.isEmpty() || ownerToken == null || ownerToken.isBlank()) {
            return;
        }
        List<String> keys = seatNumbers.stream()
                .map(s -> buildKey(tripId, s.trim().toUpperCase()))
                .distinct()
                .toList();
        releaseKeysSafely(keys, ownerToken);
    }

    @Override
    public boolean isSeatLocked(Long tripId, String seatNumber) {
        String lockKey = buildKey(tripId, seatNumber.trim().toUpperCase());
        RBucket<String> bucket = redissonClient.getBucket(lockKey, StringCodec.INSTANCE);
        return bucket.isExists();
    }

    private boolean tryAcquireSingleLock(String lockKey, String ownerToken, Duration ttl) throws InterruptedException {
        RBucket<String> bucket = redissonClient.getBucket(lockKey, StringCodec.INSTANCE);
        long deadline = System.currentTimeMillis() + WAIT_TIME_MS;
        while (true) {
            if (bucket.setIfAbsent(ownerToken, ttl)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(RETRY_INTERVAL_MS);
        }
    }

    private void releaseKeysSafely(List<String> keys, String ownerToken) {
        if (keys.isEmpty()) {
            return;
        }
        try {
            RScript script = redissonClient.getScript(StringCodec.INSTANCE);
            for (String key : keys) {
                try {
                    script.eval(
                            RScript.Mode.READ_WRITE,
                            LUA_CAS_DELETE,
                            RScript.ReturnType.INTEGER,
                            Collections.singletonList(key),
                            ownerToken
                    );
                } catch (Exception ex) {
                    log.warn("Failed to release seat lock key={}: {}", key, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to obtain Redisson RScript for releasing locks: {}", ex.getMessage());
        }
    }

    private String buildKey(Long tripId, String seatNumber) {
        return String.format("%s%d:%s", LOCK_PREFIX, tripId, seatNumber);
    }
}
