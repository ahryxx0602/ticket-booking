package com.ticket.booking.infrastructure.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSeatLockRepositoryTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> bucketA;

    @Mock
    private RBucket<String> bucketB;

    @Mock
    private RScript rScript;

    @InjectMocks
    private RedisSeatLockRepository lockRepository;

    @Test
    void acquireLocks_shouldSortSeatsAndAcquireInDeterministicOrder() {
        doReturn(bucketA).when(redissonClient).getBucket("lock:seat:1:01A", StringCodec.INSTANCE);
        doReturn(bucketB).when(redissonClient).getBucket("lock:seat:1:01B", StringCodec.INSTANCE);
        when(bucketA.setIfAbsent(eq("TOKEN-1"), eq(Duration.ofMinutes(10)))).thenReturn(true);
        when(bucketB.setIfAbsent(eq("TOKEN-1"), eq(Duration.ofMinutes(10)))).thenReturn(true);

        boolean acquired = lockRepository.acquireLocks(1L, List.of("01b", "01A", "01a"), "TOKEN-1", Duration.ofMinutes(10));

        assertThat(acquired).isTrue();
        var order = inOrder(bucketA, bucketB);
        order.verify(bucketA).setIfAbsent("TOKEN-1", Duration.ofMinutes(10));
        order.verify(bucketB).setIfAbsent("TOKEN-1", Duration.ofMinutes(10));
    }

    @Test
    void acquireLocks_whenSecondSeatFails_shouldRollbackFirstSeatWithCasScript() {
        doReturn(bucketA).when(redissonClient).getBucket("lock:seat:1:01A", StringCodec.INSTANCE);
        doReturn(bucketB).when(redissonClient).getBucket("lock:seat:1:01B", StringCodec.INSTANCE);
        when(bucketA.setIfAbsent(eq("TOKEN-1"), any())).thenReturn(true);
        when(bucketB.setIfAbsent(eq("TOKEN-1"), any())).thenReturn(false);
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(rScript);

        boolean acquired = lockRepository.acquireLocks(1L, List.of("01A", "01B"), "TOKEN-1", Duration.ofMinutes(10));

        assertThat(acquired).isFalse();
        verify(rScript).eval(
                eq(RScript.Mode.READ_WRITE),
                any(String.class),
                eq(RScript.ReturnType.INTEGER),
                eq(Collections.singletonList("lock:seat:1:01A")),
                eq("TOKEN-1")
        );
    }

    @Test
    void releaseOwnedLocks_whenRedisThrowsException_shouldNotPropagateError() {
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(rScript);
        when(rScript.eval(any(), any(), any(), anyList(), any())).thenThrow(new RuntimeException("Redis connection lost"));

        lockRepository.releaseOwnedLocks(1L, List.of("01A"), "TOKEN-1");
    }
}
