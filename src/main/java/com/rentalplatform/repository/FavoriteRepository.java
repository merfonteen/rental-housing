package com.rentalplatform.repository;

import com.rentalplatform.entity.FavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {
    List<FavoriteEntity> findAllByUserId(Long userId);
    Optional<FavoriteEntity> findByUserIdAndListingId(Long userId, Long listingId);
}
