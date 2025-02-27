package com.rentalplatform.services;

import com.rentalplatform.dto.creationDto.CreationReviewDto;
import com.rentalplatform.dto.ReviewDto;
import com.rentalplatform.dto.updateDto.UpdateReviewDto;
import com.rentalplatform.entity.*;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.mapper.ReviewDtoMapper;
import com.rentalplatform.repository.BookingRepository;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.ReviewRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.service.EmailService;
import com.rentalplatform.service.NotificationService;
import com.rentalplatform.service.RatingService;
import com.rentalplatform.service.ReviewService;
import com.rentalplatform.utils.RedisCacheCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private RatingService ratingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewDtoMapper reviewDtoMapper;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RedisCacheCleaner redisCacheCleaner;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void testGetReviewById_Success() {
        Long reviewId = 1L;

        ReviewEntity review = ReviewEntity.builder()
                .id(reviewId)
                .build();

        ReviewDto reviewDto = ReviewDto.builder()
                .id(reviewId)
                .build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.ofNullable(review));
        when(reviewDtoMapper.makeReviewDto(Objects.requireNonNull(review))).thenReturn(reviewDto);

        ReviewDto result = reviewService.getReviewById(reviewId);

        assertNotNull(result);
        assertEquals(reviewDto.getId(), result.getId());
    }

    @Test
    void testGetReviewById_WhenReviewNotFound_ShouldThrowException() {
        Long reviewId = 1L;

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.getReviewById(reviewId));
    }

    @Test
    void testGetReviewsForListing_WithoutSorting_Success() {
        Long listingId = 1L;
        boolean sortByDate = false;
        boolean sortByRating = false;
        int page = 0;
        int size = 10;
        PageRequest pageRequest = PageRequest.of(page, size);

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .title("Test Title")
                .build();

        ReviewEntity review = ReviewEntity.builder()
                .id(1L)
                .listing(listing)
                .build();

        ReviewDto reviewDto = ReviewDto.builder()
                .id(1L)
                .listingTitle("Test title")
                .build();

        Page<ReviewEntity> reviewsPage = new PageImpl<>(List.of(review), pageRequest, 1);

        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));
        when(reviewDtoMapper.makeReviewDto(review)).thenReturn(reviewDto);
        when(reviewRepository.findAllByListingId(listingId, pageRequest)).thenReturn(reviewsPage);

        Page<ReviewDto> result = reviewService.getReviewsForListing(listingId, sortByDate, sortByRating, page, size);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(reviewDto.getListingTitle(), result.getContent().get(0).getListingTitle());
        verify(listingRepository, times(1)).findById(listingId);
        verify(reviewRepository, times(1)).findAllByListingId(listingId, pageRequest);
        verify(reviewDtoMapper, times(1)).makeReviewDto(review);
    }

    @Test
    void testGetReviewsForListing_WhenThereIsSortByDate_ShouldReturnSortedListByDate() {
        Long listingId = 1L;
        boolean sortByDate = true;
        boolean sortByRating = false;
        int page = 0;
        int size = 10;
        PageRequest pageRequest = PageRequest.of(page, size);

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .title("Test Title")
                .build();

        ReviewEntity laterReview = ReviewEntity.builder()
                .id(1L)
                .listing(listing)
                .createdAt(Instant.now().minusSeconds(1800))
                .build();

        ReviewEntity earlierReview = ReviewEntity.builder()
                .id(2L)
                .listing(listing)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();

        ReviewDto laterReviewDto = ReviewDto.builder()
                .id(1L)
                .createdAt(laterReview.getCreatedAt())
                .build();

        ReviewDto earlierReviewDto = ReviewDto.builder()
                .id(2L)
                .createdAt(earlierReview.getCreatedAt())
                .build();

        Page<ReviewEntity> reviewsPage = new PageImpl<>(List.of(laterReview, earlierReview), pageRequest, 2);

        when(listingRepository.findById(listingId)).thenReturn(Optional.ofNullable(listing));
        when(reviewDtoMapper.makeReviewDto(laterReview)).thenReturn(laterReviewDto);
        when(reviewDtoMapper.makeReviewDto(earlierReview)).thenReturn(earlierReviewDto);
        when(reviewRepository.findAllByListingId(listingId, pageRequest)).thenReturn(reviewsPage);

        Page<ReviewDto> result = reviewService.getReviewsForListing(listingId, sortByDate, sortByRating, page, size);

        assertNotNull(result);
        assertEquals(earlierReviewDto.getCreatedAt(), result.getContent().get(0).getCreatedAt());
        verify(listingRepository, times(1)).findById(listingId);
        verify(reviewRepository, times(1)).findAllByListingId(listingId, pageRequest);
    }

    @Test
    void testGetReviewsForListing_WhenListingNotFound_ShouldThrowException() {
        Long listingId = 1L;

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> reviewService.getReviewsForListing(listingId, false, false, 0, 10));

        verify(listingRepository, times(1)).findById(listingId);
        verify(reviewRepository, never()).findAllByListingId(anyLong(), any(PageRequest.class));
    }

    @Test
    void testGetReviewsForListing_WhenSizeGreaterThan50_ShouldThrowException() {
        Long listingId = 1L;

        ListingEntity listing = new ListingEntity();
        listing.setId(listingId);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> reviewService.getReviewsForListing(listingId, false, false, 0, 51));

        assertEquals("Maximum page size is 50", exception.getMessage());
        verify(listingRepository, times(1)).findById(listingId);
        verify(reviewRepository, never()).findAllByListingId(anyLong(), any(PageRequest.class));
    }

    @Test
    void testCreateReview_Success() {
        String username = "Test Username";
        UserEntity tenant = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        UserEntity landlord = UserEntity.builder()
                .id(2L)
                .username("Landlord Username")
                .email("test@gmail.com")
                .build();

        CreationReviewDto creationReviewDto = CreationReviewDto.builder()
                .listingId(1L)
                .comment("Test Comment")
                .rating(10.0)
                .build();

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .title("Test Title")
                .landlord(landlord)
                .build();

        ReviewEntity review = ReviewEntity.builder()
                .id(1L)
                .listing(listing)
                .comment(creationReviewDto.getComment())
                .rating(creationReviewDto.getRating())
                .build();

        ReviewDto reviewDto = ReviewDto.builder()
                .id(1L)
                .comment(creationReviewDto.getComment())
                .rating(creationReviewDto.getRating())
                .build();

        when(listingRepository.findById(creationReviewDto.getListingId())).thenReturn(Optional.ofNullable(listing));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(tenant));
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(review);
        when(reviewDtoMapper.makeReviewDto(review)).thenReturn(reviewDto);
        when(bookingRepository.existsByListingAndTenantAndStatus(listing, tenant, BookingStatus.FINISHED))
                .thenReturn(true);

        ReviewDto result = reviewService.createReview(creationReviewDto, username);

        assertNotNull(result);
        assertEquals(reviewDto.getComment(), result.getComment());
        verify(listingRepository, times(1)).findById(creationReviewDto.getListingId());
        verify(userRepository, times(1)).findByUsername(username);
        verify(bookingRepository, times(1))
                .existsByListingAndTenantAndStatus(listing, tenant, BookingStatus.FINISHED);
        verify(reviewRepository, times(1)).save(any(ReviewEntity.class));
        verify(ratingService, times(1)).updateLandlordRating(landlord.getId());
        verify(redisCacheCleaner, times(1)).evictReviewCacheByListingId(creationReviewDto.getListingId());
        verify(emailService, times(1)).sendEmail(eq(landlord.getEmail()),
                eq("New Review Received"),
                contains(listing.getTitle()));
        verify(notificationService, times(1))
                .createNotification(contains(listing.getTitle()), eq(landlord));
    }

    @Test
    void testCreateReview_WhenListingNotFound_ShouldThrowException() {
        Long listingId = 1L;
        CreationReviewDto creationReviewDto = CreationReviewDto.builder()
                .listingId(listingId)
                .rating(8.0)
                .comment("Great listing!")
                .build();

        String criticUsername = "Tenant Username";

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                reviewService.createReview(creationReviewDto, criticUsername));

        assertEquals("Listing with id '1' not found", exception.getMessage());
        verify(listingRepository, times(1)).findById(listingId);
        verify(userRepository, never()).findByUsername(anyString());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void testCreateReview_UserNotFound() {
        CreationReviewDto creationReviewDto = CreationReviewDto.builder()
                .listingId(1L)
                .rating(8.0)
                .comment("Great listing!")
                .build();

        String criticUsername = "Tenant Username";

        ListingEntity listing = ListingEntity.builder()
                .id(creationReviewDto.getListingId())
                .title("Test Title")
                .build();

        when(listingRepository.findById(creationReviewDto.getListingId())).thenReturn(Optional.of(listing));
        when(userRepository.findByUsername(criticUsername)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                reviewService.createReview(creationReviewDto, criticUsername));

        assertEquals("User 'Tenant Username' not found", exception.getMessage());
        verify(listingRepository, times(1)).findById(creationReviewDto.getListingId());
        verify(userRepository, times(1)).findByUsername(criticUsername);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void testCreateReview_WhenCriticIsLandlord_ShouldThrowException() {
        CreationReviewDto creationReviewDto = CreationReviewDto.builder()
                .listingId(1L)
                .rating(10.0)
                .comment("Great listing!")
                .build();

        String criticUsername = "Landlord Username";
        UserEntity landlord = new UserEntity();
        landlord.setUsername(criticUsername);

        ListingEntity listing = ListingEntity.builder()
                .id(creationReviewDto.getListingId())
                .title("Test Title")
                .landlord(landlord)
                .build();

        when(listingRepository.findById(creationReviewDto.getListingId())).thenReturn(Optional.of(listing));
        when(userRepository.findByUsername(criticUsername)).thenReturn(Optional.of(landlord));

        Exception exception = assertThrows(BadRequestException.class, () ->
                reviewService.createReview(creationReviewDto, criticUsername));

        assertEquals("You cannot leave a review on your own listing", exception.getMessage());
        verify(listingRepository, times(1)).findById(creationReviewDto.getListingId());
        verify(userRepository, times(1)).findByUsername(criticUsername);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void testCreateReview_WhenUserHaveNotRentedListing_ShouldThrowException() {
        CreationReviewDto creationReviewDto = CreationReviewDto.builder()
                .listingId(1L)
                .rating(10.0)
                .comment("Great listing!")
                .build();

        String criticUsername = "Some Username";
        UserEntity user = new UserEntity();
        user.setUsername(criticUsername);

        ListingEntity listing = ListingEntity.builder()
                .id(creationReviewDto.getListingId())
                .title("Test Title")
                .landlord(UserEntity.builder().username("Landlord Username").build())
                .build();

        when(listingRepository.findById(creationReviewDto.getListingId())).thenReturn(Optional.of(listing));
        when(userRepository.findByUsername(criticUsername)).thenReturn(Optional.of(user));
        when(bookingRepository.existsByListingAndTenantAndStatus(listing, user, BookingStatus.FINISHED)).thenReturn(false);

        Exception exception = assertThrows(BadRequestException.class, () ->
                reviewService.createReview(creationReviewDto, criticUsername));

        assertEquals("You cannot leave a review for a listing that you have not rented", exception.getMessage());
        verify(listingRepository, times(1)).findById(creationReviewDto.getListingId());
        verify(userRepository, times(1)).findByUsername(criticUsername);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void testEditReview_Success() {
        Long reviewId = 1L;
        String username = "Test Username";
        UserEntity tenant = new UserEntity();
        tenant.setUsername(username);

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(UserEntity.builder()
                        .id(2L)
                        .username("Landlord Username")
                        .build())
                .build();

        UpdateReviewDto updateReviewDto = UpdateReviewDto.builder()
                .comment("Updated Comment")
                .rating(5.0)
                .build();

        ReviewEntity reviewToUpdate = ReviewEntity.builder()
                .id(1L)
                .comment("Test Comment")
                .rating(8.0)
                .tenant(tenant)
                .listing(listing)
                .build();

        ReviewDto expectedDto = ReviewDto.builder()
                .id(1L)
                .comment("Updated Comment")
                .rating(5.0)
                .build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.ofNullable(reviewToUpdate));
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(reviewToUpdate);
        when(reviewDtoMapper.makeReviewDto(Objects.requireNonNull(reviewToUpdate))).thenReturn(expectedDto);

        ReviewDto result = reviewService.editReview(reviewId, updateReviewDto, username);

        assertNotNull(result);
        assertEquals(expectedDto.getComment(), result.getComment());
        verify(reviewRepository, times(1)).findById(reviewId);
        verify(reviewRepository, times(1)).save(any(ReviewEntity.class));
        verify(redisCacheCleaner, times(1)).evictReviewCacheByListingId(reviewToUpdate.getListing().getId());
        verify(ratingService, times(1)).updateLandlordRating(listing.getLandlord().getId());
        verify(reviewDtoMapper, times(1)).makeReviewDto(reviewToUpdate);
    }

    @Test
    void testEditReview_WhenReviewNotFound_ShouldThrowException() {
        Long reviewId = 1L;
        String username = "Test Username";
        UpdateReviewDto updateReviewDto = UpdateReviewDto.builder().build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.editReview(reviewId, updateReviewDto, username));
    }

    @Test
    void testEditReview_WhenUserNotOwnerTheReview_ShouldThrowException() {
        Long reviewId = 1L;
        String username = "Another User";

        UserEntity tenant = new UserEntity();
        tenant.setUsername("Tenant Username");

        UpdateReviewDto updateReviewDto = UpdateReviewDto.builder().build();

        ReviewEntity reviewToUpdate = ReviewEntity.builder()
                .id(1L)
                .comment("Test Comment")
                .rating(8.0)
                .tenant(tenant)
                .build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.ofNullable(reviewToUpdate));

        Exception exception = assertThrows(BadRequestException.class, () ->
                reviewService.editReview(reviewId, updateReviewDto, username));

        assertEquals("You are not authorized to edit this review", exception.getMessage());
    }

    @Test
    void testDeleteReview_Success() {
        Long reviewId = 1L;
        String username = "Test Username";
        UserEntity user = new UserEntity();
        user.setUsername(username);

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(UserEntity.builder().id(1L).build())
                .build();

        ReviewEntity reviewToDelete = ReviewEntity.builder()
                .id(1L)
                .tenant(user)
                .listing(listing)
                .build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.ofNullable(reviewToDelete));

        reviewService.deleteReview(reviewId, username);

        verify(reviewRepository, times(1)).delete(Objects.requireNonNull(reviewToDelete));
        verify(redisCacheCleaner, times(1)).evictReviewCacheByListingId(reviewToDelete.getListing().getId());
        verify(ratingService, times(1)).updateLandlordRating(reviewToDelete.getListing().getLandlord().getId());
    }

    @Test
    void testDeleteReview_WhenReviewNotFound_ShouldThrowException() {
        Long reviewId = 1L;
        String username = "Test Username";

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.deleteReview(reviewId, username));
    }

    @Test
    void testDeleteReview_WhenUserNotOwnerTheReview_ShouldThrowException() {
        Long reviewId = 1L;
        String username = "Another User";

        UserEntity tenant = new UserEntity();
        tenant.setUsername("Tenant Username");

        ReviewEntity reviewToDelete = ReviewEntity.builder()
                .id(1L)
                .tenant(tenant)
                .build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.ofNullable(reviewToDelete));

        Exception exception = assertThrows(BadRequestException.class, () ->
                reviewService.deleteReview(reviewId, username));

        assertEquals("You are not authorized to delete this review", exception.getMessage());
    }
}