package com.rentalplatform.factory;

import com.rentalplatform.dto.BookingDto;
import com.rentalplatform.entity.BookingEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookingDtoFactory {
    public List<BookingDto> makeBookingDto(List<BookingEntity> bookings) {
        return bookings.stream()
                .map(this::makeBookingDto)
                .collect(Collectors.toList());
    }

    public BookingDto makeBookingDto(BookingEntity booking) {
        return BookingDto.builder()
                .id(booking.getId())
                .listingTitle(booking.getListing().getTitle())
                .tenantUsername(booking.getTenant().getUsername())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .status(booking.getStatus())
                .build();
    }
}
