package com.rentalplatform.mapper;

import com.rentalplatform.dto.ListingDto;
import com.rentalplatform.entity.ListingEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ListingDtoMapper {

    private final ReviewDtoMapper reviewDtoMapper;

    public ListingDto makeListingDto(ListingEntity listing) {
        return ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .address(listing.getAddress())
                .price(listing.getPrice())
                .type(listing.getType())
                .ownerUsername(listing.getLandlord().getUsername())
                .createdAt(listing.getCreatedAt())
                .nextAvailableDateForBooking(listing.getNextAvailableDate())
                .build();
    }

    public List<ListingDto> makeListingDto(List<ListingEntity> listings) {
        return listings.stream()
                .map(this::makeListingDto)
                .collect(Collectors.toList());
    }

    public ListingDto makeListingDtoWithReviews(ListingEntity listing) {
        return ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .address(listing.getAddress())
                .price(listing.getPrice())
                .type(listing.getType())
                .ownerUsername(listing.getLandlord().getUsername())
                .createdAt(listing.getCreatedAt())
                .nextAvailableDateForBooking(listing.getNextAvailableDate())
                .reviews(
                        listing.getReviews()
                                .stream()
                                .map(reviewDtoMapper::makeReviewDto)
                                .collect(Collectors.toList()))
                .build();
    }

    public List<ListingDto> makeListingDtoWithReviews(List<ListingEntity> listings) {
        return listings.stream()
                .map(this::makeListingDtoWithReviews)
                .collect(Collectors.toList());
    }
}
