package com.rentalplatform.factory;

import com.rentalplatform.dto.ListingDto;
import com.rentalplatform.entity.ListingEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ListingDtoFactory {

    private final ReviewDtoFactory reviewDtoFactory;

    public List<ListingDto> makeListingDto(List<ListingEntity> listings) {
        return listings.stream()
                .map(this::makeListingDto)
                .collect(Collectors.toList());
    }

    public ListingDto makeListingDto(ListingEntity listing) {
        return ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .price(listing.getPrice())
                .type(listing.getType())
                .ownerUsername(listing.getLandlord().getUsername())
                .createdAt(listing.getCreatedAt())
                .nextAvailableDateForBooking(listing.getNextAvailableDate())
                .build();
    }

    public List<ListingDto> makeListingDtoWithReviews(List<ListingEntity> listings) {
        return listings.stream()
                .map(this::makeListingDtoWithReviews)
                .collect(Collectors.toList());
    }

    public ListingDto makeListingDtoWithReviews(ListingEntity listing) {
        return ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .price(listing.getPrice())
                .type(listing.getType())
                .ownerUsername(listing.getLandlord().getUsername())
                .createdAt(listing.getCreatedAt())
                .nextAvailableDateForBooking(listing.getNextAvailableDate())
                .reviews(
                        listing.getReviews()
                                .stream()
                                .map(reviewDtoFactory::makeReviewDto)
                                .collect(Collectors.toList()))
                .build();
    }
}
