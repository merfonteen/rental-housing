package com.rentalplatform.mapper;

import com.rentalplatform.dto.ReviewDto;
import com.rentalplatform.entity.ReviewEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReviewDtoMapper {
    public ReviewDto makeReviewDto(ReviewEntity review) {
        return ReviewDto.builder()
                .id(review.getId())
                .listingTitle(review.getListing().getTitle())
                .criticUsername(review.getTenant().getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    public List<ReviewDto> makeReviewDto(List<ReviewEntity> reviews) {
        return reviews.stream()
                .map(this::makeReviewDto)
                .collect(Collectors.toList());
    }
}
