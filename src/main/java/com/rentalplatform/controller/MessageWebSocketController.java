package com.rentalplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class MessageWebSocketController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public void sendNotification(String receiverUsername, String message) {
        simpMessagingTemplate.convertAndSendToUser(
                receiverUsername,
                "/topic/notifications",
                message
        );
    }
}
