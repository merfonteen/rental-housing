package com.rentalplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MessageDto implements Serializable {
    private Long id;
    private String content;
    private String senderUsername;
    private String receiverUsername;
    private boolean isRead;
    private Instant createdAt;
}
