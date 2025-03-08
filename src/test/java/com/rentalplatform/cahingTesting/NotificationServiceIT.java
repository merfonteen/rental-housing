package com.rentalplatform.cahingTesting;

import com.rentalplatform.entity.NotificationEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.repository.NotificationRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.service.NotificationService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationServiceIT extends AbstractRedisTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> redisOps;

    @BeforeEach
    void setUp() {
        redisOps = redisTemplate.opsForValue();
    }

    @Test
    @Transactional
    void testGetNotificationById_ShouldCacheResult() {
        NotificationEntity notification = creteTestNotificationForUser("testUsername");
        String cacheKey = "notifications::" + notification.getId();

        notificationService.getNotificationById(notification.getId(), notification.getUser().getUsername());
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    @Transactional
    void testGetAllNotifications_ShouldCacheResult() {
        int page = 0;
        int size = 10;
        NotificationEntity notification = creteTestNotificationForUser("testUsername");
        String cacheKey = "notifications::" + notification.getUser().getUsername() + "_" + page + "_" + size;

        notificationService.getAllNotifications(notification.getUser().getUsername(), page, size);
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    @Transactional
    void testGetUnreadNotifications_ShouldCacheResult() {
        int page = 0;
        int size = 10;
        NotificationEntity notification = creteTestNotificationForUser("testUsername");
        String cacheKey = "unreadNotifications::" + notification.getUser().getUsername() + "_" + page + "_" + size;

        notificationService.getUnreadNotifications(notification.getUser().getUsername(), page, size);
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    @Transactional
    void testCreateNotification_ShouldEvictCache() {
        String username = "testUser";
        NotificationEntity notification = creteTestNotificationForUser(username);

        List<String> cachesToEvict = List.of(
                "notifications::" + username + "_0_10",
                "unreadNotifications::" + username + "_0_10"
        );

        notificationService.getAllNotifications(username, 0, 10);
        notificationService.getUnreadNotifications(username, 0, 10);

        performCacheEvictionTest(
                cachesToEvict,
                () -> notificationService.createNotification("testMessage", notification.getUser())
        );
    }

    @Test
    @Transactional
    void testMarkAsRead_ShouldEvictCache() {
        String username = "testUser";
        NotificationEntity notification = creteTestNotificationForUser(username);

        List<String> cachesToEvict = List.of(
                "notifications::" + notification.getId(),
                "notifications::" + username + "_0_10",
                "unreadNotifications::" + username + "_0_10"
        );

        notificationService.getNotificationById(notification.getId(), username);
        notificationService.getAllNotifications(username, 0, 10);
        notificationService.getUnreadNotifications(username, 0, 10);

        performCacheEvictionTest(
                cachesToEvict,
                () -> notificationService.markAsRead(notification.getId(), notification.getUser().getUsername())
        );
    }

    private void performCacheEvictionTest(List<String> cachesToEvict, Runnable action) {
        for (String cacheKey : cachesToEvict) {
            assertThat(redisOps.get(cacheKey)).isNotNull();
        }

        action.run();

        for (String cacheKey : cachesToEvict) {
            assertThat(redisOps.get(cacheKey)).isNull();
        }
    }

    private NotificationEntity creteTestNotificationForUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .username(username)
                        .email("testEmail123@gmail.com")
                        .password("123456")
                        .build())
                );

        NotificationEntity notification = notificationRepository.findById(1L)
                .orElseGet(() -> notificationRepository.save(NotificationEntity.builder()
                        .message("Test message")
                        .user(user)
                        .isRead(false)
                        .build()));

        return notification;
    }
}
