package com.rentalplatform.dto.creationDto;

import com.rentalplatform.entity.ListingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CreationListingDto {
    @NotBlank(message = "Title is required!")
    private String title;

    @NotBlank(message = "Description is required!")
    private String description;

    @NotNull(message = "Price is required!")
    @Positive(message = "The price must be greater than 0")
    private Double price;

    @NotBlank(message = "Address is required!")
    private String address;

    @NotNull(message = "Number of rooms is required!")
    @Positive(message = "The number of rooms must be greater than 0")
    private Integer numberOfRooms;

    @NotNull(message = "Type is required!")
    private ListingType type;
}
