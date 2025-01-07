package com.rentalplatform.service;

import com.rentalplatform.dto.ListingDto;
import com.rentalplatform.entity.FavoriteEntity;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.factory.ListingDtoFactory;
import com.rentalplatform.repository.FavoriteRepository;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class FavoriteService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final FavoriteRepository favoriteRepository;
    private final ListingDtoFactory listingDtoFactory;

    public List<ListingDto> getFavoriteListings(String username) {
        UserEntity user = getUser(username);

        List<FavoriteEntity> favorites = favoriteRepository.findAllByUserId(user.getId());

        List<ListingEntity> listings = favorites.stream()
                .map(FavoriteEntity::getListing)
                .toList();

        return listingDtoFactory.makeListingDto(listings);
    }

    @Transactional
    public ListingDto addToFavorites(Long listingId, String username) {
        ListingEntity listingToAdd = getListing(listingId);

        UserEntity user = getUser(username);

        favoriteRepository.findByUserIdAndListingId(user.getId(), listingId)
                .ifPresent(another -> {
                    throw new BadRequestException("Listing is already in your favorites");
                });

        favoriteRepository.save(
                FavoriteEntity.builder()
                        .user(user)
                        .listing(listingToAdd)
                        .build());

        return listingDtoFactory.makeListingDto(listingToAdd);
    }

    @Transactional
    public void removeFromFavorites(Long listingId, String username) {
        UserEntity user = getUser(username);
        ListingEntity listing = getListing(listingId);

        favoriteRepository.findByUserIdAndListingId(user.getId(), listingId)
                .ifPresentOrElse(favoriteRepository::delete, () -> {
                    throw new BadRequestException("Listing not found in your favorites");
                });
    }

    private UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(username)));
    }

    private ListingEntity getListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing with id '%d' not found".formatted(listingId)));
    }
}
