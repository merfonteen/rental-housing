package com.rentalplatform.repository;

import com.rentalplatform.entity.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<ImageEntity, Long> {
    Optional<ImageEntity> findByFilename(String filename);
    List<ImageEntity> findAllByListingId(Long listingId);
}
