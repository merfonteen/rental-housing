package com.rentalplatform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Builder
@Data
public class MessageDto {
    private Long id;
    private String content;
    private String senderUsername;
    private String receiverUsername;
    private boolean isRead;
    private Instant createdAt;
}
