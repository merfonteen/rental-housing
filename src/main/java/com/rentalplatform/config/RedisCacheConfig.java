package com.rentalplatform.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@EnableCaching
@Configuration
public class RedisCacheConfig {
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("bookings", redisCacheConfiguration().entryTtl(Duration.ofMinutes(20)));
        cacheConfigurations.put("bookingsForLandlord", redisCacheConfiguration().entryTtl(Duration.ofMinutes(20)));
        cacheConfigurations.put("favoriteListings", redisCacheConfiguration().entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("notifications", redisCacheConfiguration().entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("unreadNotifications", redisCacheConfiguration().entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("messages", redisCacheConfiguration().entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("unreadMessages", redisCacheConfiguration().entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("reviews", redisCacheConfiguration().entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration())
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
