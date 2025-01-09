package com.rentalplatform.service;

import com.rentalplatform.dto.CreationReviewDto;
import com.rentalplatform.dto.ReviewDto;
import com.rentalplatform.dto.UpdateReviewDto;
import com.rentalplatform.entity.*;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.factory.ReviewDtoFactory;
import com.rentalplatform.repository.BookingRepository;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.ReviewRepository;
import com.rentalplatform.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ReviewService {

    private final EmailService emailService;
    private final RatingService ratingService;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewDtoFactory reviewDtoFactory;
    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final NotificationService notificationService;

    public Page<ReviewDto> getReviewsForListing(Long listingId, int page, int size) {
        listingRepository.findById(listingId).orElseThrow(
                () -> new NotFoundException("Listing with id '%d' not found".formatted(listingId)));

        if (size > 50) {
            throw new BadRequestException("Maximum page size is 50");
        }

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ReviewEntity> reviews = reviewRepository.findAllByListingId(listingId, pageRequest);
        return reviews.map(reviewDtoFactory::makeReviewDto);
    }

    @Transactional
    public ReviewDto createReview(CreationReviewDto creationReviewDto, String username) {
        ListingEntity listing = findListingById(creationReviewDto.getListingId());

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

        emailService.sendEmail(listing.getLandlord().getEmail(),
                "New Review Received",
                "Your listing '%s' has received a new review from %s.\n Comment: %s"
                        .formatted(listing.getTitle(), review.getTenant(), review.getComment()));

        notificationService.createNotification("Your listing '%s' has received a new review from %s.\n Comment: %s"
                        .formatted(listing.getTitle(), review.getTenant(), review.getComment()),
                listing.getLandlord());

        return reviewDtoFactory.makeReviewDto(savedReview);
    }

    @Transactional
    public ReviewDto editReviewDto(Long reviewId, UpdateReviewDto updateReviewDto, String username) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review with id '%d' not found".formatted(reviewId)));

        validateUpdatingReview(updateReviewDto, username, review);

        ReviewEntity savedReview = reviewRepository.save(review);
        ratingService.updateLandlordRating(review.getListing().getLandlord().getId());
        return reviewDtoFactory.makeReviewDto(savedReview);
    }

    @Transactional
    public boolean deleteReview(Long reviewId, String username) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review with id '%d' not found".formatted(reviewId)));

        if (!review.getTenant().getUsername().equals(username)) {
            throw new BadRequestException("You are not authorized to delete this review");
        }

        reviewRepository.delete(review);
        ratingService.updateLandlordRating(review.getListing().getLandlord().getId());
        return true;
    }

    private ListingEntity findListingById(Long listingId) {
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
}
