package com.rentalplatform.dto;

import com.rentalplatform.entity.ListingType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class FilterListingsDto {
    private String title;
    private String address;
    @PositiveOrZero(message = "Minimal price must be greater or equal 0")
    private Double minPrice;
    @PositiveOrZero(message = "Maximum price must be greater or equal 0")
    private Double maxPrice;
    private Integer numberOfRooms;
    private ListingType type;
    @Min(1)
    @Max(5)
    private Double minAverageRating;
    private LocalDate availableFrom;
}
