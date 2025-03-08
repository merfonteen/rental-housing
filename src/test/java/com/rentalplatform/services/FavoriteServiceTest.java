package com.rentalplatform.services;

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
import com.rentalplatform.service.FavoriteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingDtoMapper listingDtoMapper;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void testGetFavoriteById_Success() {
        Long favoriteId = 1L;

        ListingEntity listing = ListingEntity.builder()
                .id(50L)
                .title("Test Title")
                .build();

        FavoriteEntity favorite = FavoriteEntity.builder()
                .id(favoriteId)
                .listing(listing)
                .build();

        ListingDto expectedDto = ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .build();

        when(favoriteRepository.findById(favoriteId)).thenReturn(Optional.of(favorite));
        when(listingDtoMapper.makeListingDto(listing)).thenReturn(expectedDto);
        when(listingDtoMapper.makeListingDto(listing)).thenReturn(expectedDto);

        ListingDto result = favoriteService.getFavoriteById(favoriteId);

        assertNotNull(result);
        assertEquals(expectedDto.getId(), result.getId());
        assertEquals(expectedDto.getTitle(), result.getTitle());
        verify(favoriteRepository, times(1)).findById(favoriteId);
        verify(listingDtoMapper, times(1)).makeListingDto(listing);
    }

    @Test
    void testGetFavoriteById_NotFound() {
        Long favoriteId = 1L;
        when(favoriteRepository.findById(favoriteId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> favoriteService.getFavoriteById(favoriteId));
    }

    @Test
    void testGetFavoriteListings_Success() {
        String username = "Current Username";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .build();

        ListingDto listingDto = ListingDto.builder()
                .id(1L)
                .build();

        FavoriteEntity favorite = FavoriteEntity.builder()
                .id(1L)
                .user(user)
                .listing(listing)
                .build();

        List<FavoriteEntity> favoriteListings = new ArrayList<>(List.of(favorite));
        List<ListingEntity> listings = new ArrayList<>(List.of(listing));
        List<ListingDto> listingDtos = new ArrayList<>(List.of(listingDto));

        when(userRepository.findByUsername(username)).thenReturn(Optional.ofNullable(user));
        when(favoriteRepository.findAllByUserId(user.getId())).thenReturn(favoriteListings);
        when(listingDtoMapper.makeListingDto(listings)).thenReturn(listingDtos);

        List<ListingDto> result = favoriteService.getFavoriteListings(username);

        assertNotNull(result);
        assertEquals(listingDto.getId(), result.get(0).getId());
        verify(userRepository, times(1)).findByUsername(username);
        verify(favoriteRepository, times(1)).findAllByUserId(user.getId());
        verify(listingDtoMapper, times(1)).makeListingDto(listings);
    }

    @Test
    void testGetFavoriteListings_UserNotFound() {
        String username = "nonExistingUser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> favoriteService.getFavoriteListings(username));
    }

    @Test
    void testAddToFavorites_Success() {
        Long listingId = 1L;
        String username = "Test Username";

        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .build();

        ListingDto listingDto = ListingDto.builder()
                .id(1L)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(favoriteRepository.findByUserIdAndListingId(user.getId(), listingId)).thenReturn(Optional.empty());
        when(listingDtoMapper.makeListingDto(Objects.requireNonNull(listing))).thenReturn(listingDto);

        ListingDto result = favoriteService.addToFavorites(listingId, username);

        assertNotNull(result);
        assertEquals(listingDto.getId(), result.getId());
        verify(favoriteRepository, times(1)).save(any(FavoriteEntity.class));
    }

    @Test
    void testAddToFavorites_WhenListingNotFound_ShouldThrowException() {
        Long listingId = 1L;
        String username = "Test Username";

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> favoriteService.addToFavorites(listingId, username));

        assertEquals("Listing with id '1' not found", exception.getMessage());
    }

    @Test
    void testAddToFavorites_WhenUserNotFound_ShouldThrowException() {
        Long listingId = 1L;
        String username = "Test Username";

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .title("Test Listing")
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> favoriteService.addToFavorites(listingId, username));

        assertEquals("User 'Test Username' not found", exception.getMessage());
    }

    @Test
    void testAddToFavorites_WhenFavoriteAlreadyExists_ShouldThrowException() {
        Long listingId = 1L;
        String username = "Test Username";

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .title("Test Listing")
                .build();

        UserEntity user = UserEntity.builder()
                .id(10L)
                .username(username)
                .build();

        FavoriteEntity favorite = FavoriteEntity.builder()
                .id(100L)
                .user(user)
                .listing(listing)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(favoriteRepository.findByUserIdAndListingId(user.getId(), listingId))
                .thenReturn(Optional.of(favorite));

        Exception exception = assertThrows(BadRequestException.class, () ->
                favoriteService.addToFavorites(listingId, username));

        assertEquals("Listing is already in your favorites", exception.getMessage());
    }

    @Test
    void testRemoveFromFavorites_Success() {
        Long favoriteListingId = 1L;
        String username = "Test Username";

        UserEntity user = UserEntity.builder()
                .id(10L)
                .username(username)
                .build();

        FavoriteEntity favorite = FavoriteEntity.builder()
                .id(100L)
                .user(user)
                .build();

        when(favoriteRepository.findById(favoriteListingId)).thenReturn(Optional.ofNullable(favorite));

        favoriteService.removeFromFavorites(favoriteListingId, username);

        verify(favoriteRepository, times(1)).delete(favorite);
        verify(favoriteRepository, times(1)).findById(favoriteListingId);
    }

    @Test
    void testRemoveFromFavorites_WhenFavoriteListingNotFound_ShouldThrowException() {
        Long listingId = 1L;
        String username = "Test Username";

        when(favoriteRepository.findById(listingId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class,
                () -> favoriteService.removeFromFavorites(listingId, username));

        assertEquals("Favorite listing with id '1' not found", exception.getMessage());
    }

    @Test
    void testRemoveFromFavorites_WhenUnauthorized_ShouldThrowException() {
        Long favoriteListingId = 1L;
        String username = "Test Username";

        FavoriteEntity favoriteListing = FavoriteEntity.builder()
                .id(favoriteListingId)
                .user(UserEntity.builder().username("anotherUser").build())
                .build();

        when(favoriteRepository.findById(favoriteListingId)).thenReturn(Optional.of(favoriteListing));

        Exception exception = assertThrows(BadRequestException.class,
                () -> favoriteService.removeFromFavorites(favoriteListingId, username));

        assertEquals("You are not authorized to remove this listing from favorites", exception.getMessage());
    }
}