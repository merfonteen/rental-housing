package com.rentalplatform.service;

import com.rentalplatform.dto.CreationListingDto;
import com.rentalplatform.dto.EditListingDto;
import com.rentalplatform.dto.FilterListingsDto;
import com.rentalplatform.dto.ListingDto;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.factory.ListingDtoFactory;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ListingService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ListingDtoFactory listingDtoFactory;

    public List<ListingDto> getMyListings(String currentUsername) {
        UserEntity landlord = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BadRequestException("User '%s' not found".formatted(currentUsername)));

        List<ListingEntity> listings = listingRepository.findAllByLandlord(landlord);

        return listingDtoFactory.makeListingDto(listings);
    }

    public List<ListingDto> getAllListings(FilterListingsDto filter) {
        List<ListingEntity> listings = listingRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(filter.getTitle() != null && !filter.getTitle().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("title"), "%" + filter.getTitle() + "%"));
            }

            if(filter.getAddress() != null && !filter.getAddress().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("address"), "%" + filter.getAddress() + "%"));
            }

            if(filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }

            if(filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }

            if(filter.getNumberOfRooms() != null) {
                predicates.add(criteriaBuilder.equal(root.get("numberOfRooms"), filter.getNumberOfRooms()));
            }

            if(filter.getType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), filter.getType()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });

        return listingDtoFactory.makeListingDto(listings);
    }

    public ListingDto createListing(CreationListingDto creationListingDto, String currentUsername) {
        UserEntity landlord = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BadRequestException("User '%s' not found".formatted(currentUsername)));

        if (listingRepository.existsByTitleAndAddressAndLandlord(
                creationListingDto.getTitle(),
                creationListingDto.getAddress(),
                landlord)) {
            throw new BadRequestException("You've already created a listing with such title and address");
        }

        ListingEntity listing = listingRepository.save(
                ListingEntity.builder()
                        .title(creationListingDto.getTitle())
                        .description(creationListingDto.getDescription())
                        .price(creationListingDto.getPrice())
                        .address(creationListingDto.getAddress())
                        .numberOfRooms(creationListingDto.getNumberOfRooms())
                        .type(creationListingDto.getType())
                        .landlord(landlord)
                        .isAvailable(true)
                        .build()
        );

        return listingDtoFactory.makeListingDto(listing);
    }

    public ListingDto editListing(Long listingId, EditListingDto editListingDto, String currentUsername) {
        ListingEntity listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing with id '%d' not found".
                        formatted(listingId)));

        if(!listing.getLandlord().getUsername().equals(currentUsername)) {
            throw new BadRequestException("You are not the owner of this listing");
        }

        if(editListingDto.getTitle() != null && !editListingDto.getTitle().isEmpty()) {
            listing.setTitle(editListingDto.getTitle());
        }

        if(editListingDto.getDescription() != null && !editListingDto.getDescription().isEmpty()) {
            listing.setDescription(editListingDto.getDescription());
        }

        if(editListingDto.getPrice() != null && editListingDto.getPrice() > 0) {
            listing.setPrice(editListingDto.getPrice());
        }

        if(editListingDto.getAddress() != null && !editListingDto.getAddress().isEmpty()) {
            listing.setAddress(editListingDto.getAddress());
        }

        if(editListingDto.getNumberOfRooms() != null && editListingDto.getNumberOfRooms() > 0) {
            listing.setNumberOfRooms(editListingDto.getNumberOfRooms());
        }

        if(editListingDto.getType() != null) {
            listing.setType(editListingDto.getType());
        }

        ListingEntity updatedListing = listingRepository.save(listing);

        return listingDtoFactory.makeListingDto(updatedListing);
    }

    public void deleteListing(Long listingId, String currentUsername) {
        ListingEntity listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing with id '%d' not found".
                        formatted(listingId)));

        if(!listing.getLandlord().getUsername().equals(currentUsername)) {
            throw new BadRequestException("You are not the owner of this listing");
        }

        listingRepository.delete(listing);
    }
}
