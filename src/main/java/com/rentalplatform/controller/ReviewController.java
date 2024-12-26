package com.rentalplatform.controller;

import com.rentalplatform.dto.CreationReviewDto;
import com.rentalplatform.dto.ReviewDto;
import com.rentalplatform.dto.UpdateReviewDto;
import com.rentalplatform.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/reviews")
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/listing/{listingId}")
    public ResponseEntity<List<ReviewDto>> getReviewsForListing(@PathVariable Long listingId) {
        return ResponseEntity.ok(reviewService.getReviewsForListing(listingId));
    }

    @PostMapping
    public ResponseEntity<ReviewDto> createReview(@Valid @RequestBody CreationReviewDto creationReviewDto,
                                                  Principal principal) {
        return ResponseEntity.ok(reviewService.createReview(creationReviewDto, principal.getName()));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> editReview(@PathVariable Long reviewId,
                                                @Valid @RequestBody UpdateReviewDto reviewDto,
                                                Principal principal) {
        return ResponseEntity.ok(reviewService.editReviewDto(reviewId, reviewDto, principal.getName()));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Boolean> deleteReview(@PathVariable Long reviewId, Principal principal) {
        return ResponseEntity.ok(reviewService.deleteReview(reviewId, principal.getName()));
    }
}
