package com.rentalplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    private boolean isRead;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
