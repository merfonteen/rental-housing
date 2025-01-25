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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDtoMapper notificationDtoMapper;
    private final RedisCacheCleaner redisCacheCleaner;
    private final NotificationWebSocketController notificationWebSocketController;

    @Cacheable(cacheNames = "notifications", key = "#notificationId")
    public NotificationDto getNotificationById(Long notificationId, String username) {
        NotificationEntity notification = findNotificationById(notificationId);

        if(!username.equals(notification.getUser().getUsername())) {
            throw new BadRequestException("You are not authorized to view this notification");
        }

        return notificationDtoMapper.makeNotificationDto(notification);
    }

    @Cacheable(cacheNames = "notifications", key = "#username + '_' + #page + '_' + #size")
    public Page<NotificationDto> getAllNotifications(String username, int page, int size) {
        UserEntity currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(username)));

        if(size > 50) {
            throw new BadRequestException("Maximum page size is 50");
        }

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<NotificationEntity> notifications = notificationRepository.findAllByUserId(currentUser.getId(), pageRequest);

        return notifications.map(notificationDtoMapper::makeNotificationDto);
    }

    @Cacheable(cacheNames = "unreadNotifications", key = "#username + '_' + #page + '_' + #size")
    public Page<NotificationDto> getUnreadNotifications(String username, int page, int size) {
        UserEntity currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(username)));

        if(size > 50) {
            throw new BadRequestException("Maximum page size is 50");
        }

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<NotificationEntity> notifications = notificationRepository
                .findAllByUserIdAndIsReadFalse(currentUser.getId(), pageRequest);

        return notifications.map(notificationDtoMapper::makeNotificationDto);
    }

    public void createNotification(String message, UserEntity user) {
        NotificationEntity notification = NotificationEntity.builder()
                .message(message)
                .user(user)
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        redisCacheCleaner.evictCacheForNotification(user.getUsername());

        notificationWebSocketController.sendNotification(user.getUsername(), message);
    }

    @CacheEvict(cacheNames = "notifications", key = "#notificationId")
    public void markAsRead(Long notificationId) {
        NotificationEntity notification = findNotificationById(notificationId);
        notification.setRead(true);
        redisCacheCleaner.evictCacheForNotification(notification.getUser().getUsername());
    }

    private NotificationEntity findNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification with id '%d' not found".formatted(notificationId)));
    }
}
