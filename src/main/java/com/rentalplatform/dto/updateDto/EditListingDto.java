package com.rentalplatform.dto.updateDto;

import com.rentalplatform.entity.ListingType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EditListingDto {
    private String title;
    private String description;
    private Double price;
    private String address;
    private Integer numberOfRooms;
    private ListingType type;
}
