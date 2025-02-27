package com.rentalplatform.services;

import com.rentalplatform.dto.creationDto.CreationListingDto;
import com.rentalplatform.dto.updateDto.EditListingDto;
import com.rentalplatform.dto.FilterListingsDto;
import com.rentalplatform.dto.ListingDto;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.ListingType;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.mapper.ListingDtoMapper;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.service.ListingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingDtoMapper listingDtoMapper;

    @InjectMocks
    private ListingService listingService;

    @Test
    void testGetListingById_Success() {
        Long listingId = 1L;

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .build();

        ListingDto listingDto = ListingDto.builder()
                .id(listingId)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));
        when(listingDtoMapper.makeListingDto(listing)).thenReturn(listingDto);

        ListingDto result = listingService.getListingById(listingId);

        assertNotNull(result);
        assertEquals(listingDto.getId(), result.getId());
        verify(listingRepository, times(1)).findById(listingId);
        verify(listingDtoMapper, times(1)).makeListingDto(listing);
    }

    @Test
    void testGetListingById_WhenListingNotFound_ShouldThrowException() {
        Long listingId = 1L;

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> listingService.getListingById(listingId));
        verify(listingRepository, times(1)).findById(listingId);
        verify(listingDtoMapper, never()).makeListingDto(any(ListingEntity.class));
    }

    @Test
    void testGetMyListings_Success() {
        String username = "Landlord Username";
        int page = 0;
        int size = 10;
        PageRequest request = PageRequest.of(page, size);

        UserEntity landlord = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .title("Test Title")
                .landlord(landlord)
                .build();

        ListingDto listingDto = ListingDto.builder()
                .id(1L)
                .title("Test Title")
                .build();

        Page<ListingEntity> listingsPage = new PageImpl<>(List.of(listing), request, 1);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(landlord));
        when(listingRepository.findAllWithReviewsByLandlordId(landlord.getId(), request)).thenReturn(listingsPage);
        when(listingDtoMapper.makeListingDtoWithReviews(listing)).thenReturn(listingDto);

        Page<ListingDto> result = listingService.getMyListings(username, page, size);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository, times(1)).findByUsername(username);
        verify(listingRepository, times(1)).findAllWithReviewsByLandlordId(landlord.getId(), request);
        verify(listingDtoMapper, times(1)).makeListingDtoWithReviews(listing);
    }

    @Test
    void testGetMyListings_WhenUserNotFound_ShouldThrowException() {
        String username = "Landlord Username";
        int page = 0;
        int size = 10;

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> listingService.getMyListings(username, page, size));
    }

    @Test
    void testGetMyListings_WhenSizeGreaterThan50_ShouldThrowException() {
        String username = "Test Username";
        int page = 0;
        int size = 51;

        UserEntity landlord = new UserEntity();
        landlord.setUsername(username);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(landlord));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> listingService.getMyListings(username, page, size));

        assertEquals("Maximum page size is 50", exception.getMessage());
        verify(listingRepository, never()).findAllWithReviewsByLandlordId(anyLong(), any(PageRequest.class));
    }

    @Test
    void testGetAllListings_Success() {
        int page = 0;
        int size = 10;
        PageRequest pageRequest = PageRequest.of(page, size);
        FilterListingsDto filter = FilterListingsDto.builder()
                .title("Test")
                .address("Test Address")
                .minPrice(100.0)
                .maxPrice(300.0)
                .numberOfRooms(2)
                .type(ListingType.APARTMENT)
                .minAverageRating(3.5)
                .availableFrom(LocalDate.of(2025, 1, 1))
                .build();

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .title("Test Title")
                .build();

        ListingDto listingDto = ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .build();

        Page<ListingEntity> listingPage = new PageImpl<>(List.of(listing), pageRequest, 1);

        when(listingRepository.findAll(any(Specification.class), eq(pageRequest)))
                .thenReturn(listingPage);
        when(listingDtoMapper.makeListingDtoWithReviews(listing)).thenReturn(listingDto);

        Page<ListingDto> result = listingService.getAllListings(filter, page, size);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(listingRepository, times(1)).findAll(any(Specification.class), eq(pageRequest));
        verify(listingDtoMapper, times(1)).makeListingDtoWithReviews(listing);
    }

    @Test
    void testGetAllListings_SizeGreaterThan50() {
        int page = 0;
        int size = 51;
        FilterListingsDto filter = FilterListingsDto.builder().build();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> listingService.getAllListings(filter, page, size));
        assertEquals("Maximum page size is 50", exception.getMessage());

        verify(listingRepository, never()).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void testCreateListing_Success() {
        String username = "Test Username";

        UserEntity landlord = new UserEntity();
        landlord.setUsername(username);

        CreationListingDto creationListingDto = CreationListingDto.builder()
                .title("Test Title")
                .description("Test Description")
                .price(5000.0)
                .address("Test Address")
                .numberOfRooms(2)
                .type(ListingType.APARTMENT)
                .build();

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .title(creationListingDto.getTitle())
                .address(creationListingDto.getAddress())
                .build();

        ListingDto listingDto = ListingDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .address(listing.getAddress())
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(landlord));
        when(listingRepository.save(any(ListingEntity.class))).thenReturn(listing);
        when(listingDtoMapper.makeListingDto(listing)).thenReturn(listingDto);
        when(listingRepository.existsByTitleAndAddressAndLandlord(
                creationListingDto.getTitle(),
                creationListingDto.getAddress(),
                landlord)
        ).thenReturn(false);

        ListingDto result = listingService.createListing(creationListingDto, username);

        assertNotNull(result);
        assertEquals(listingDto.getTitle(), result.getTitle());
        verify(userRepository, times(1)).findByUsername(username);
        verify(listingRepository, times(1)).save(any(ListingEntity.class));
        verify(listingDtoMapper, times(1)).makeListingDto(listing);
        verify(listingRepository, times(1)).existsByTitleAndAddressAndLandlord(
                creationListingDto.getTitle(),
                creationListingDto.getAddress(),
                landlord);
    }

    @Test
    void testCreateListing_WhenUserNotFound_ShouldThrowException() {
        String username = "Landlord Username";
        CreationListingDto creationListingDto = CreationListingDto.builder().build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> listingService.createListing(creationListingDto, username));
    }

    @Test
    void testCreateListing_WhenListingAlreadyExistsFromTheSameUser_ShouldThrowException() {
        String username = "Test Username";

        UserEntity landlord = new UserEntity();
        landlord.setUsername(username);

        CreationListingDto creationListingDto = CreationListingDto.builder()
                .title("Test Title")
                .address("Test Address")
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(landlord));
        when(listingRepository.existsByTitleAndAddressAndLandlord(
                creationListingDto.getTitle(),
                creationListingDto.getAddress(),
                landlord)
        ).thenReturn(true);

        Exception exception = assertThrows(BadRequestException.class, () ->
                listingService.createListing(creationListingDto, username));

        assertEquals("You've already created a listing with such title and address", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(username);
        verify(listingRepository, times(1))
                .existsByTitleAndAddressAndLandlord(
                        creationListingDto.getTitle(),
                        creationListingDto.getAddress(),
                        landlord);
        verify(listingRepository, never()).save(any());
    }

    @Test
    void testEditListing_Success() {
        Long listingId = 1L;
        String username = "Landlord Username";
        UserEntity landlord = new UserEntity();
        landlord.setUsername(username);

        EditListingDto editDto = EditListingDto.builder()
                .title("New Test Title")
                .description("New Test Description")
                .price(5001.0)
                .build();

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .title("Test Title")
                .description("Test Description")
                .price(5000.0)
                .landlord(landlord)
                .build();

        ListingDto listingDto = ListingDto.builder()
                .id(1L)
                .title(editDto.getTitle())
                .description(editDto.getDescription())
                .price(editDto.getPrice())
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));
        when(listingRepository.save(any(ListingEntity.class))).thenReturn(listing);
        when(listingDtoMapper.makeListingDto(listing)).thenReturn(listingDto);

        ListingDto result = listingService.editListing(listingId, editDto, username);

        assertNotNull(result);
        assertEquals(listingDto.getTitle(), result.getTitle());
        verify(listingRepository, times(1)).findById(listingId);
        verify(listingRepository, times(1)).save(listing);
        verify(listingDtoMapper, times(1)).makeListingDto(listing);
    }

    @Test
    void testEditListing_WhenListingNotFound_ShouldThrowException() {
        Long listingId = 1L;
        String username = "Landlord Username";
        EditListingDto dto = EditListingDto.builder().build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> listingService.editListing(listingId, dto, username));
    }

    @Test
    void testEditListing_WhenUserNotOwnerTheListing_ShouldThrowException() {
        Long listingId = 1L;
        String username = "Landlord Username";
        EditListingDto dto = EditListingDto.builder().build();

        UserEntity anotherUser = new UserEntity();
        anotherUser.setUsername("Another Username");

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(anotherUser)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));

        Exception exception = assertThrows(BadRequestException.class, () ->
                listingService.editListing(listingId, dto, username));

        assertEquals("You are not the owner of this listing", exception.getMessage());
        verify(listingRepository, times(1)).findById(listingId);
        verify(listingRepository, never()).save(listing);
        verify(listingDtoMapper, never()).makeListingDto(listing);
    }

    @Test
    void testDeleteListing_Success() {
        Long listingId = 1L;
        String username = "Landlord Username";

        UserEntity landlord = new UserEntity();
        landlord.setUsername(username);

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(landlord)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));

        listingService.deleteListing(listingId, username);

        verify(listingRepository, times(1)).findById(listingId);
        verify(listingRepository, times(1)).delete(listing);
    }

    @Test
    void testDeleteListing_WhenListingNotFound_ShouldThrowException() {
        Long listingId = 1L;
        String username = "Test Username";

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> listingService.deleteListing(listingId, username));

        verify(listingRepository, times(1)).findById(listingId);
        verify(listingRepository, never()).delete(any(ListingEntity.class));
    }

    @Test
    void testDeleteListing_WhenUserNotOwnerTheListing_ShouldThrowException() {
        Long listingId = 1L;
        String username = "Landlord Username";

        UserEntity anotherUser = new UserEntity();
        anotherUser.setUsername("Another Username");

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(anotherUser)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));

        Exception exception = assertThrows(BadRequestException.class, () ->
                listingService.deleteListing(listingId, username));

        assertEquals("You are not the owner of this listing", exception.getMessage());
        verify(listingRepository, times(1)).findById(listingId);
        verify(listingRepository, never()).delete(any(ListingEntity.class));
    }
}