package com.ticket.catalog.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.catalog.application.dto.TripDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Cache-Aside Pattern implementation with Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripRedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String CACHE_PREFIX = "trip:search:";

    public List<TripDto> getCachedSearch(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(CACHE_PREFIX + cacheKey);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<List<TripDto>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis read failure for key: {}", cacheKey, e);
        }
        return Collections.emptyList();
    }

    public void setCachedSearch(String cacheKey, List<TripDto> trips, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(trips);
            redisTemplate.opsForValue().set(CACHE_PREFIX + cacheKey, json, ttl);
        } catch (Exception e) {
            log.warn("Redis write failure for key: {}", cacheKey, e);
        }
    }
}
