package com.rentalplatform.controller;

import com.rentalplatform.dto.NotificationDto;
import com.rentalplatform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationDto> getNotification(@PathVariable Long notificationId, Principal principal) {
        return ResponseEntity.ok(notificationService.getNotificationById(notificationId, principal.getName()));
    }

    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationDto>> getUnreadNotifications(Principal principal,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(principal.getName(), page, size));
    }

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getAllNotifications(Principal principal,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getAllNotifications(principal.getName(), page, size));
    }

    @PatchMapping("/read/{notificationId}")
    public ResponseEntity<String> markAsRead(@PathVariable Long notificationId, Principal principal) {
        notificationService.markAsRead(notificationId, principal.getName());
        return ResponseEntity.ok("Notification marked as read");
    }
}
