package com.rentalplatform.service;

import com.rentalplatform.controller.NotificationWebSocketController;
import com.rentalplatform.dto.NotificationDto;
import com.rentalplatform.entity.NotificationEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.factory.NotificationDtoFactory;
import com.rentalplatform.repository.NotificationRepository;
import com.rentalplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDtoFactory notificationDtoFactory;
    private final NotificationWebSocketController notificationWebSocketController;

    public void createNotification(String message, UserEntity user) {
        NotificationEntity notification = NotificationEntity.builder()
                .message(message)
                .user(user)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        notificationWebSocketController.sendNotification(user.getUsername(), message);
    }

    public List<NotificationDto> getUnreadNotifications(String username) {
        UserEntity currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(username)));

        return notificationDtoFactory.makeNotificationDto(
                notificationRepository.findAllByUserIdAndIsReadFalse(currentUser.getId()));
    }

    public List<NotificationDto> getAllNotifications(String username) {
        UserEntity currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(username)));

        return notificationDtoFactory.makeNotificationDto(notificationRepository.findAllByUserId(currentUser.getId()));
    }

    public void markAsRead(Long notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification with id '%d' not found"
                        .formatted(notificationId)));
        notification.setRead(true);
    }
}
