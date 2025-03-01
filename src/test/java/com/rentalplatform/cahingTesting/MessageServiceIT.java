package com.rentalplatform.cahingTesting;

import com.rentalplatform.entity.MessageEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.repository.MessageRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.service.MessageService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageServiceIT extends AbstractRedisTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> redisOps;

    @BeforeEach
    void setUp() {
        redisOps = redisTemplate.opsForValue();
    }

    @Test
    @Transactional
    void testGetAllMessages_ShouldCacheResult() {
        MessageEntity message = createTestMessageForUser("testUsername");
        String cacheKey = "messages::" + message.getReceiver().getUsername();

        messageService.getAllMessages(message.getReceiver().getUsername());
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    @Transactional
    void testGetUnreadMessages_ShouldCacheResult() {
        MessageEntity message = createTestMessageForUser("testUsername");
        String cacheKey = "unreadMessages::" + message.getReceiver().getUsername();

        messageService.getUnreadMessages(message.getReceiver().getUsername());
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    @Transactional
    void testSendMessage_ShouldEvictCache() {
        MessageEntity message = createTestMessageForUser("testUsername");
        String cacheKeyForUser1 = "messages::" + message.getSender().getUsername();
        String cacheKeyForUser2 = "unreadMessages::" + message.getReceiver().getUsername();

        messageService.getAllMessages(message.getSender().getUsername());
        messageService.getUnreadMessages(message.getReceiver().getUsername());
        assertThat(redisOps.get(cacheKeyForUser1)).isNotNull();
        assertThat(redisOps.get(cacheKeyForUser2)).isNotNull();

        messageService.sendMessage(
                message.getReceiver().getUsername(),
                "content",
                message.getSender().getUsername()
        );

        assertThat(redisOps.get(cacheKeyForUser1)).isNull();
        assertThat(redisOps.get(cacheKeyForUser2)).isNull();
    }

    @Test
    @Transactional
    void testMarkAsRead_ShouldEvictCache() {
        MessageEntity message = createTestMessageForUser("testUsername");
        String cacheKey = "unreadMessages::" + message.getReceiver().getUsername();

        messageService.markAsRead(message.getId(), message.getReceiver().getUsername());
        assertThat(redisOps.get(cacheKey)).isNull();
    }

    private MessageEntity createTestMessageForUser(String username) {
        UserEntity user1 = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .username(username)
                        .email("user1@gmail.com")
                        .password("123456")
                        .build())
                );

        UserEntity user2 = userRepository.findByUsername("testUser2")
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .username("testUser2")
                        .email("user2@gmail.com")
                        .password("123456")
                        .build())
                );

        MessageEntity messageForUser1 = messageRepository.findById(1L)
                .orElseGet(() -> messageRepository.save(MessageEntity.builder()
                        .receiver(user1)
                        .sender(user2)
                        .content("test content for user1")
                        .isRead(false)
                        .build()));

        MessageEntity messageForUser2 = messageRepository.findById(1L)
                .orElseGet(() -> messageRepository.save(MessageEntity.builder()
                        .receiver(user2)
                        .sender(user1)
                        .content("test content for user2")
                        .isRead(false)
                        .build()));

        return messageForUser1;
    }
}
