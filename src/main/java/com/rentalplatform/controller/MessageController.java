package com.rentalplatform.controller;

import com.rentalplatform.dto.MessageDto;
import com.rentalplatform.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/messages")
@RestController
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/conversation/{receiverUsername}")
    public ResponseEntity<List<MessageDto>> getConversation(@PathVariable String receiverUsername,
                                                            Principal principal) {
        return ResponseEntity.ok(messageService.getConversation(receiverUsername, principal.getName()));
    }

    @GetMapping
    public ResponseEntity<List<MessageDto>> getAllMessages(Principal principal) {
        return ResponseEntity.ok(messageService.getAllMessages(principal.getName()));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<MessageDto>> getUnreadMessages(Principal principal) {
        return ResponseEntity.ok(messageService.getUnreadMessages(principal.getName()));
    }

    @PostMapping("/{receiverUsername}")
    public ResponseEntity<String> sendMessage(@PathVariable String receiverUsername,
                                              @RequestBody String content,
                                              Principal principal) {
        messageService.sendMessage(receiverUsername, content, principal.getName());
        return ResponseEntity.ok("Message sent successfully");
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<MessageDto> markAsRead(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(messageService.markAsRead(id, principal.getName()));
    }
}
