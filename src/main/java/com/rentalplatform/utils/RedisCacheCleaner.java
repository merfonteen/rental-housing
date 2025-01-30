package com.rentalplatform.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisCacheCleaner {

    private final RedisTemplate<String, Object> redisTemplate;

    public void evictCacheForReviewByListingId(Long listingId) {
        evictCacheByPattern("reviews::" + listingId + "-*");
    }

    public void evictBookingCacheForUser(String username) {
        evictCacheByPattern("bookings::" + username + "-*");
    }

    public void evictBookingCacheForLandlord(String username) {
        evictCacheByPattern("landlordBookings::" + username + "-*");
    }

    public void evictCacheForNotification(String username) {
        evictCacheByPattern("notifications::" + username + "-*");
    }

    public void evictCacheByPattern(String pattern) {
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();

        try (Cursor<byte[]> cursor = connection.scan(
                ScanOptions.scanOptions().match(pattern).count(1000).build())) {

            while (cursor.hasNext()) {
                byte[] key = cursor.next();
                redisTemplate.delete(new String(key));
            }

        } catch (Exception e) {
            log.error("Error while scanning Redis keys");
            throw new RuntimeException("Error while scanning Redis keys", e);
        }
    }
}
