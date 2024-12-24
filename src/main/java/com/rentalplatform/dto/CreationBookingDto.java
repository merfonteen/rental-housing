package com.rentalplatform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class CreationBookingDto {
    @NotNull(message = "Listing ID is required")
    private Long listingId;
    @NotNull(message = "Booking start date is required")
    private LocalDate startDate;
    @NotNull(message = "Booking end date is required")
    private LocalDate endDate;
}
