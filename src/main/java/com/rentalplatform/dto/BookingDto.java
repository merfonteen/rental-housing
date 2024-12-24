package com.rentalplatform.dto;

import com.rentalplatform.entity.BookingStatus;
import com.rentalplatform.service.BookingService;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Builder
@Data
public class BookingDto {
    private Long id;
    private String listingTitle;
    private String tenantUsername;
    private Instant startDate;
    private Instant endDate;
    private BookingStatus status;
}
