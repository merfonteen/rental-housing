package com.rentalplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "review")
public class ReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "listing_id")
    private ListingEntity listing;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private UserEntity tenant;

    private Integer rating;

    private String comment;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
