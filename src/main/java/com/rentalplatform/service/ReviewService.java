package com.rentalplatform.service;

import com.rentalplatform.dto.ReviewDto;
import com.rentalplatform.dto.creationDto.CreationReviewDto;
import com.rentalplatform.dto.updateDto.UpdateReviewDto;
import com.rentalplatform.entity.BookingStatus;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.ReviewEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.mapper.ReviewDtoMapper;
import com.rentalplatform.repository.BookingRepository;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.ReviewRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.utils.RedisCacheCleaner;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ReviewService {

    private final EmailService emailService;
    private final RatingService ratingService;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewDtoMapper reviewDtoMapper;
    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final NotificationService notificationService;
    private final RedisCacheCleaner redisCacheCleaner;

    @Cacheable(cacheNames = "reviews", key = "#reviewId", unless = "#result == null")
    public ReviewDto getReviewById(Long reviewId) {
        ReviewEntity review = findReviewByIdOrThrowException(reviewId);
        return reviewDtoMapper.makeReviewDto(review);
    }

    @Cacheable(cacheNames = "reviews",
               key = "#listingId + '_' + #sortByDate + '_' + #sortByRating + '_' + #page + '_' + #size",
               unless = "#result.content.isEmpty()"
    )
    public Page<ReviewDto> getReviewsForListing(Long listingId, boolean sortByDate, boolean sortByRating,
                                                int page, int size) {
        findListingByIdOrThrowException(listingId);

        if (size > 50) {
            throw new BadRequestException("Maximum page size is 50");
        }

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ReviewEntity> reviews = reviewRepository.findAllByListingId(listingId, pageRequest);

       Comparator<ReviewEntity> comparator = determineComparator(sortByDate, sortByRating);

       if(comparator != null) {
           return sortByComparator(reviews, comparator, pageRequest).map(reviewDtoMapper::makeReviewDto);
       }

        return reviews.map(reviewDtoMapper::makeReviewDto);
    }

    @Transactional
    public ReviewDto createReview(CreationReviewDto creationReviewDto, String username) {
        ListingEntity listing = findListingByIdOrThrowException(creationReviewDto.getListingId());

        UserEntity critic = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(username)));

        validateReviewCreation(username, listing, critic);

        ReviewEntity review = ReviewEntity.builder()
                .listing(listing)
                .tenant(critic)
                .rating(creationReviewDto.getRating())
                .comment(creationReviewDto.getComment())
                .build();

        ReviewEntity savedReview = reviewRepository.save(review);

        ratingService.updateLandlordRating(listing.getLandlord().getId());

        redisCacheCleaner.evictReviewCacheByListingId(creationReviewDto.getListingId());

        emailService.sendEmail(listing.getLandlord().getEmail(),
                "New Review Received",
                "Your listing '%s' has received a new review from %s.\n Comment: %s"
                        .formatted(listing.getTitle(), review.getTenant(), review.getComment()));

        notificationService.createNotification("Your listing '%s' has received a new review from %s.\n Comment: %s"
                        .formatted(listing.getTitle(), review.getTenant(), review.getComment()),
                listing.getLandlord());

        return reviewDtoMapper.makeReviewDto(savedReview);
    }

    @CacheEvict(cacheNames = "reviews", key = "#reviewId")
    @Transactional
    public ReviewDto editReview(Long reviewId, UpdateReviewDto updateReviewDto, String username) {
        ReviewEntity review = findReviewByIdOrThrowException(reviewId);
        validateUpdatingReview(updateReviewDto, username, review);

        ReviewEntity savedReview = reviewRepository.save(review);

        redisCacheCleaner.evictReviewCacheByListingId(review.getListing().getId());
        ratingService.updateLandlordRating(review.getListing().getLandlord().getId());

        return reviewDtoMapper.makeReviewDto(savedReview);
    }

    @CacheEvict(cacheNames = "reviews", key = "#reviewId")
    @Transactional
    public void deleteReview(Long reviewId, String username) {
        ReviewEntity review = findReviewByIdOrThrowException(reviewId);

        if (!review.getTenant().getUsername().equals(username)) {
            throw new BadRequestException("You are not authorized to delete this review");
        }

        reviewRepository.delete(review);
        redisCacheCleaner.evictReviewCacheByListingId(review.getListing().getId());
        ratingService.updateLandlordRating(review.getListing().getLandlord().getId());
    }

    private ReviewEntity findReviewByIdOrThrowException(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review with id '%d' not found".formatted(reviewId)));
    }

    private ListingEntity findListingByIdOrThrowException(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new NotFoundException("Listing with id '%d' not found".formatted(listingId)));
    }

    private void validateReviewCreation(String username, ListingEntity listing, UserEntity critic) {
        if (listing.getLandlord().getUsername().equals(username)) {
            throw new BadRequestException("You cannot leave a review on your own listing");
        }

        if (!bookingRepository.existsByListingAndTenantAndStatus(listing, critic, BookingStatus.FINISHED)) {
            throw new BadRequestException("You cannot leave a review for a listing that you have not rented");
        }
    }

    private static void validateUpdatingReview(UpdateReviewDto updateReviewDto, String username, ReviewEntity review) {
        if (!review.getTenant().getUsername().equals(username)) {
            throw new BadRequestException("You are not authorized to edit this review");
        }

        if (updateReviewDto.getRating() != null) {
            review.setRating(updateReviewDto.getRating());
        }

        if (updateReviewDto.getComment() != null && !updateReviewDto.getComment().isEmpty()) {
            review.setComment(updateReviewDto.getComment());
        }
    }

    private static Comparator<ReviewEntity> determineComparator(boolean sortByDate, boolean sortByRating) {
        if(sortByDate && sortByRating) {
            return Comparator.comparing(ReviewEntity::getCreatedAt).thenComparing(ReviewEntity::getRating);
        }
        else if(sortByDate) {
            return Comparator.comparing(ReviewEntity::getCreatedAt);
        }
        else if(sortByRating) {
            return Comparator.comparing(ReviewEntity::getRating);
        }
        return null;
    }

    private static Page<ReviewEntity> sortByComparator(Page<ReviewEntity> reviews,
                                                Comparator<ReviewEntity> comparator,
                                                PageRequest pageRequest) {
        List<ReviewEntity> sortedReviews = reviews.stream()
                .sorted(comparator)
                .toList();
        return new PageImpl<>(sortedReviews, pageRequest, reviews.getTotalElements());
    }
}
