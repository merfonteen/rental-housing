package com.rentalplatform.service;

import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.repository.ReviewRepository;
import com.rentalplatform.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RatingService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public void updateLandlordRating(Long landlordId) {
        UserEntity landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new NotFoundException("User with id '%s' not found".formatted(landlordId)));
        Double averageRating = reviewRepository.findAverageRatingForLandlord(landlordId);
        landlord.setRating(averageRating != null ? averageRating : 0.0);
        userRepository.save(landlord);
    }
}
