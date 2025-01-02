package com.rentalplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class NotificationWebSocketController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public void sendNotification(String username, String message) {
        simpMessagingTemplate.convertAndSendToUser(
                username,
                "/topic/notifications",
                message
        );
    }
}
