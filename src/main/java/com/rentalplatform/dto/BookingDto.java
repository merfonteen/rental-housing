package com.rentalplatform.dto;

import com.rentalplatform.entity.BookingStatus;
import com.rentalplatform.service.BookingService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
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
