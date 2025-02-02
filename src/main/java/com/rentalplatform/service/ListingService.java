package com.rentalplatform.service;

import com.rentalplatform.dto.CreationListingDto;
import com.rentalplatform.dto.EditListingDto;
import com.rentalplatform.dto.FilterListingsDto;
import com.rentalplatform.dto.ListingDto;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.mapper.ListingDtoMapper;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.utils.ListingSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ListingService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ListingDtoMapper listingDtoMapper;

    public ListingDto getListingById(Long listingId) {
        ListingEntity listing = findListingById(listingId);
        return listingDtoMapper.makeListingDto(listing);
    }

    public Page<ListingDto> getMyListings(String username, int page, int size) {
        UserEntity landlord = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(username)));

        if(size > 50) {
            throw new BadRequestException("Maximum page size is 50");
        }

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ListingEntity> listings = listingRepository.findAllWithReviewsByLandlordId(landlord.getId(), pageRequest);

        return listings.map(listingDtoMapper::makeListingDtoWithReviews);
    }

    public Page<ListingDto> getAllListings(FilterListingsDto filter, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        if(size > 50) {
            throw new BadRequestException("Maximum page size is 50");
        }

        Specification<ListingEntity> specification = Specification
                .where(ListingSpecification.hasTitle(filter.getTitle()))
                .and(ListingSpecification.hasAddress(filter.getAddress()))
                .and(ListingSpecification.hasMinPrice(filter.getMinPrice()))
                .and(ListingSpecification.hasMaxPrice(filter.getMaxPrice()))
                .and(ListingSpecification.hasNumberOfRooms(filter.getNumberOfRooms()))
                .and(ListingSpecification.hasType(filter.getType()))
                .and(ListingSpecification.hasMinAverageRating(filter.getMinAverageRating()))
                .and(ListingSpecification.hasAvailableFrom(filter.getAvailableFrom()));

        Page<ListingEntity> listings = listingRepository.findAll(specification, pageRequest);

        return listings.map(listingDtoMapper::makeListingDtoWithReviews);
    }

    @Transactional
    public ListingDto createListing(CreationListingDto creationListingDto, String currentUsername) {
        UserEntity landlord = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(currentUsername)));

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
                        .build()
        );

        return listingDtoMapper.makeListingDto(listing);
    }

    @Transactional
    public ListingDto editListing(Long listingId, EditListingDto editListingDto, String currentUsername) {
        ListingEntity listing = findListingById(listingId);

        validateUpdatingListing(editListingDto, currentUsername, listing);

        ListingEntity updatedListing = listingRepository.save(listing);

        return listingDtoMapper.makeListingDto(updatedListing);
    }

    @Transactional
    public void deleteListing(Long listingId, String currentUsername) {
        ListingEntity listing = findListingById(listingId);
        validateLandlordListing(currentUsername, listing);
        listingRepository.delete(listing);
    }

    private ListingEntity findListingById(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing with id '%d' not found".
                        formatted(listingId)));
    }

    private static void validateUpdatingListing(EditListingDto editListingDto, String currentUsername, ListingEntity listing) {
        validateLandlordListing(currentUsername, listing);

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
    }

    private static void validateLandlordListing(String currentUsername, ListingEntity listing) {
        if(!listing.getLandlord().getUsername().equals(currentUsername)) {
            throw new BadRequestException("You are not the owner of this listing");
        }
    }
}
