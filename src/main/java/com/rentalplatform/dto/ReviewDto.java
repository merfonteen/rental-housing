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
public class ReviewDto implements Serializable {
    private Long id;
    private String listingTitle;
    private String criticUsername;
    private Double rating;
    private String comment;
    private Instant createdAt;
}
