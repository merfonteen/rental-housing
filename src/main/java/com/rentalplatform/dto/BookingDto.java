package com.rentalplatform.dto;

import com.rentalplatform.entity.BookingStatus;
import com.rentalplatform.service.BookingService;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Builder
@Data
public class BookingDto implements Serializable {
    private Long id;
    private String listingTitle;
    private String tenantUsername;
    private Instant startDate;
    private Instant endDate;
    private BookingStatus status;
}
