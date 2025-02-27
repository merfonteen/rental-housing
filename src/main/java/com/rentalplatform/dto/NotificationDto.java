package com.rentalplatform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class NotificationDto implements Serializable {
    private Long id;
    private String message;
    private boolean isRead;
    private String username;
    @JsonProperty("created_at")
    private Instant createdAt;
}
