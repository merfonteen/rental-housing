package com.rentalplatform.factory;

import com.rentalplatform.dto.ListingDto;
import com.rentalplatform.entity.ListingEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ListingDtoFactory {
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
                .build();
    }
}
