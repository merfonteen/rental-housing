package com.rentalplatform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Builder
@Data
public class ReviewDto {
    private Long id;
    private String listingTitle;
    private String criticUsername;
    private Double rating;
    private String comment;
    private Instant createdAt;
}
