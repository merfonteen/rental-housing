package com.rentalplatform.controller;

import com.rentalplatform.dto.NotificationDto;
import com.rentalplatform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(Principal principal) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(principal.getName()));
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getAllNotifications(Principal principal) {
        return ResponseEntity.ok(notificationService.getAllNotifications(principal.getName()));
    }

    @PatchMapping("/read/{id}")
    public ResponseEntity<String> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok("Notification marked as read");
    }
}
