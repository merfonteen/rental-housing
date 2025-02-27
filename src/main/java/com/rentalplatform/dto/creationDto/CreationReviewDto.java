package com.rentalplatform.dto.creationDto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class CreationReviewDto implements Serializable {
    @NotNull(message = "Listing ID is required")
    private Long listingId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 10, message = "Rating cannot exceed 10")
    private Double rating;

    @NotBlank(message = "Comment is required")
    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;
}
