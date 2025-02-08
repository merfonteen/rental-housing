package com.rentalplatform.service;

import com.rentalplatform.controller.NotificationWebSocketController;
import com.rentalplatform.dto.NotificationDto;
import com.rentalplatform.entity.NotificationEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.mapper.NotificationDtoMapper;
import com.rentalplatform.repository.NotificationRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.utils.RedisCacheCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisCacheCleaner redisCacheCleaner;

    @Mock
    private NotificationDtoMapper notificationDtoMapper;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationWebSocketController notificationWebSocketController;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void testGetNotificationById_Success() {
        Long notificationId = 1L;
        String username = "Test Username";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        NotificationEntity notification = NotificationEntity.builder()
                .id(notificationId)
                .message("Test message")
                .user(user)
                .isRead(false)
                .build();

        NotificationDto expectedDto = NotificationDto.builder()
                .id(notificationId)
                .message("Test message")
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationDtoMapper.makeNotificationDto(notification)).thenReturn(expectedDto);

        NotificationDto result = notificationService.getNotificationById(notificationId, username);

        assertNotNull(result);
        assertEquals(expectedDto.getMessage(), result.getMessage());
        verify(notificationRepository, times(1)).findById(notificationId);
        verify(notificationDtoMapper, times(1)).makeNotificationDto(notification);
    }

    @Test
    void testGetNotificationById_WhenUserUnauthorized_ShouldThrowException() {
        Long notificationId = 1L;
        String username = "Test Username";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("Other User")
                .build();

        NotificationEntity notification = NotificationEntity.builder()
                .id(notificationId)
                .message("Test message")
                .user(user)
                .isRead(false)
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        Exception exception = assertThrows(BadRequestException.class,
                () -> notificationService.getNotificationById(notificationId, username));

        assertEquals("You are not authorized to view this notification", exception.getMessage());
    }

    @Test
    void testGetNotificationById_WhenNotificationNotFound_ShouldThrowException() {
        Long notificationId = 1L;
        String username = "Test Username";

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class,
                () -> notificationService.getNotificationById(notificationId, username));

        assertEquals("Notification with id '1' not found", exception.getMessage());
    }

    @Test
    void testGetAllNotifications_Success() {
        String username = "Test Username";
        int page = 0;
        int size = 10;
        PageRequest pageRequest = PageRequest.of(page, size);

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        NotificationEntity notification1 = NotificationEntity.builder()
                .id(1L)
                .message("Message 1")
                .isRead(false)
                .user(user)
                .build();

        NotificationEntity notification2 = NotificationEntity.builder()
                .id(2L)
                .message("Message 2")
                .isRead(true)
                .user(user)
                .build();

        List<NotificationEntity> notificationList = List.of(notification1, notification2);
        Page<NotificationEntity> notificationPage = new PageImpl<>(notificationList, pageRequest, notificationList.size());

        NotificationDto dto1 = NotificationDto.builder()
                .id(notification1.getId())
                .message(notification1.getMessage())
                .build();

        NotificationDto dto2 = NotificationDto.builder()
                .id(notification2.getId())
                .message(notification2.getMessage())
                .build();
        when(notificationDtoMapper.makeNotificationDto(notification1)).thenReturn(dto1);
        when(notificationDtoMapper.makeNotificationDto(notification2)).thenReturn(dto2);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(notificationRepository.findAllByUserId(user.getId(), pageRequest)).thenReturn(notificationPage);

        Page<NotificationDto> result = notificationService.getAllNotifications(username, page, size);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(dto1, result.getContent().get(0));
        assertEquals(dto2, result.getContent().get(1));
        verify(userRepository, times(1)).findByUsername(username);
        verify(notificationRepository, times(1)).findAllByUserId(user.getId(), pageRequest);
        verify(notificationDtoMapper, times(1)).makeNotificationDto(notification1);
        verify(notificationDtoMapper, times(1)).makeNotificationDto(notification2);
    }

    @Test
    void testGetAllNotifications_WhenSizeGreaterThan50_ShouldThrowException() {
        String username = "Test Username";
        int page = 0;
        int size = 51;

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        Exception exception = assertThrows(BadRequestException.class,
                () -> notificationService.getAllNotifications(username, page, size));

        assertEquals("Maximum page size is 50", exception.getMessage());
    }

    @Test
    void testGetAllNotifications_WhenUserNotFound_ShouldThrowException() {
        String username = "NonExistingUser";
        int page = 0;
        int size = 10;

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class,
                () -> notificationService.getAllNotifications(username, page, size));

        assertEquals("User 'NonExistingUser' not found", exception.getMessage());
    }

    @Test
    void testGetUnreadNotifications_Success() {
        String username = "Test Username";
        int page = 0;
        int size = 10;
        PageRequest pageRequest = PageRequest.of(page, size);

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        NotificationEntity notification1 = NotificationEntity.builder()
                .id(1L)
                .message("Unread Message 1")
                .isRead(false)
                .user(user)
                .build();

        NotificationEntity notification2 = NotificationEntity.builder()
                .id(2L)
                .message("Unread Message 2")
                .isRead(false)
                .user(user)
                .build();

        List<NotificationEntity> notificationList = List.of(notification1, notification2);
        Page<NotificationEntity> notificationPage = new PageImpl<>(notificationList, pageRequest, notificationList.size());

        NotificationDto dto1 = NotificationDto.builder()
                .id(notification1.getId())
                .message(notification1.getMessage())
                .build();

        NotificationDto dto2 = NotificationDto.builder()
                .id(notification2.getId())
                .message(notification2.getMessage())
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(notificationDtoMapper.makeNotificationDto(notification1)).thenReturn(dto1);
        when(notificationDtoMapper.makeNotificationDto(notification2)).thenReturn(dto2);
        when(notificationRepository.findAllByUserIdAndIsReadFalse(user.getId(), pageRequest))
                .thenReturn(notificationPage);

        Page<NotificationDto> result = notificationService.getUnreadNotifications(username, page, size);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(dto1, result.getContent().get(0));
        assertEquals(dto2, result.getContent().get(1));
        verify(userRepository, times(1)).findByUsername(username);
        verify(notificationRepository, times(1)).findAllByUserIdAndIsReadFalse(user.getId(), pageRequest);
        verify(notificationDtoMapper, times(1)).makeNotificationDto(notification1);
        verify(notificationDtoMapper, times(1)).makeNotificationDto(notification2);
    }

    @Test
    void testGetUnreadNotifications_WhenSizeGreaterThan50_ShouldThrowException() {
        String username = "Test Username";
        int page = 0;
        int size = 51;

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        Exception exception = assertThrows(BadRequestException.class,
                () -> notificationService.getUnreadNotifications(username, page, size));

        assertEquals("Maximum page size is 50", exception.getMessage());
    }

    @Test
    void testGetUnreadNotifications_WhenUserNotFound_ShouldThrowException() {
        String username = "NonExistingUser";
        int page = 0;
        int size = 10;

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class,
                () -> notificationService.getUnreadNotifications(username, page, size));

        assertEquals("User 'NonExistingUser' not found", exception.getMessage());
    }

    @Test
    void testCreateNotification_Success() {
        String message = "New notification message";
        String username = "Test Username";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        NotificationEntity savedNotification = NotificationEntity.builder()
                .id(1L)
                .message(message)
                .user(user)
                .isRead(false)
                .build();

        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(savedNotification);

        notificationService.createNotification(message, user);

        verify(notificationRepository, times(1)).save(any(NotificationEntity.class));
        verify(redisCacheCleaner, times(1)).evictNotificationCacheByUsername(username);
        verify(notificationWebSocketController, times(1)).sendNotification(username, message);
    }

    @Test
    void testMarkAsRead_Success() {
        Long notificationId = 1L;
        String username = "Test Username";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        NotificationEntity notification = NotificationEntity.builder()
                .id(notificationId)
                .message("Test message")
                .user(user)
                .isRead(false)
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId);

        assertTrue(notification.isRead());
        verify(redisCacheCleaner, times(1)).evictNotificationCacheByUsername(username);
    }

    @Test
    void testMarkAsRead_WhenNotificationNotFound_ShouldThrowException() {
        Long notificationId = 1L;

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class, () ->
                notificationService.markAsRead(notificationId));

        assertEquals("Notification with id '1' not found", exception.getMessage());
    }
}