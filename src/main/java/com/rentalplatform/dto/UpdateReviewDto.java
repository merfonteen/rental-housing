package com.rentalplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateReviewDto {
    @Min(1)
    @Max(10)
    private Double rating;

    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;
}
