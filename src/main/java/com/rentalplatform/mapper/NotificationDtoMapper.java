package com.rentalplatform.mapper;

import com.rentalplatform.dto.NotificationDto;
import com.rentalplatform.entity.NotificationEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NotificationDtoMapper {
    public List<NotificationDto> makeNotificationDto(List<NotificationEntity> notifications) {
        return notifications.stream()
                .map(this::makeNotificationDto)
                .collect(Collectors.toList());
    }

    public NotificationDto makeNotificationDto(NotificationEntity notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .username(notification.getUser().getUsername())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
