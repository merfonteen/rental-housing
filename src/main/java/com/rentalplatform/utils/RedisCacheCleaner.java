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

    public void evictReviewCacheByListingId(Long listingId) {
        evictCacheByPattern("reviews::" + listingId + "_*");
    }

    public void evictBookingCacheForUser(String username) {
        evictCacheByPattern("bookings::" + username + "_*");
    }

    public void evictBookingCacheForLandlord(String landlordUsername) {
        evictCacheByPattern("bookingsForLandlord::" + landlordUsername + "_*");
    }

    public void evictNotificationCacheByUsername(String username) {
        evictCacheByPattern("notifications::" + username + "_*");
    }

    public void evictUnreadNotificationsCacheByUsername(String username) {
        evictCacheByPattern("unreadNotifications::" + username + "_*");
    }


    public void evictCacheByPattern(String pattern) {
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();

        try (Cursor<byte[]> cursor = connection.scan(
                ScanOptions.scanOptions().match(pattern).count(1000).build())) {

            while (cursor.hasNext()) {
                byte[] key = cursor.next();
                String redisKey = new String(key);
                redisTemplate.delete(redisKey);
            }

        } catch (Exception e) {
            log.error("Error while scanning Redis keys");
            throw new RuntimeException("Error while scanning Redis keys", e);
        }
    }
}
