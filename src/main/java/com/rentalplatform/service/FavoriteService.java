package com.rentalplatform.service;

import com.rentalplatform.dto.ListingDto;
import com.rentalplatform.entity.FavoriteEntity;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.mapper.ListingDtoMapper;
import com.rentalplatform.repository.FavoriteRepository;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class FavoriteService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final FavoriteRepository favoriteRepository;
    private final ListingDtoMapper listingDtoMapper;

    public ListingDto getFavoriteById(Long favoriteId) {
        FavoriteEntity favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new NotFoundException("Favorite listing with id '%d' not found".formatted(favoriteId)));
        return listingDtoMapper.makeListingDto(favorite.getListing());
    }

    @Cacheable(cacheNames = "favoriteListings", key = "#username")
    public List<ListingDto> getFavoriteListings(String username) {
        UserEntity user = findUserByUsernameOrThrowException(username);

        List<FavoriteEntity> favorites = favoriteRepository.findAllByUserId(user.getId());

        List<ListingEntity> listings = favorites.stream()
                .map(FavoriteEntity::getListing)
                .toList();

        return listingDtoMapper.makeListingDto(listings);
    }

    @CacheEvict(cacheNames = "favoriteListings", key = "#username")
    @Transactional
    public ListingDto addToFavorites(Long listingId, String username) {
        ListingEntity listingToAdd = findListingByIdOrThrowException(listingId);
        UserEntity user = findUserByUsernameOrThrowException(username);

        favoriteRepository.findByUserIdAndListingId(user.getId(), listingId)
                .ifPresent(another -> {
                    throw new BadRequestException("Listing is already in your favorites");
                });

        favoriteRepository.save(
                FavoriteEntity.builder()
                        .user(user)
                        .listing(listingToAdd)
                        .build());

        return listingDtoMapper.makeListingDto(listingToAdd);
    }

    @CacheEvict(cacheNames = "favoriteListings", key = "#username")
    @Transactional
    public void removeFromFavorites(Long listingId, String username) {
        findListingByIdOrThrowException(listingId);
        UserEntity user = findUserByUsernameOrThrowException(username);

        favoriteRepository.findByUserIdAndListingId(user.getId(), listingId)
                .ifPresentOrElse(favoriteRepository::delete, () -> {
                    throw new BadRequestException("Listing not found in your favorites");
                });
    }

    private UserEntity findUserByUsernameOrThrowException(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(username)));
    }

    private ListingEntity findListingByIdOrThrowException(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing with id '%d' not found".formatted(listingId)));
    }
}
