package com.rentalplatform.repository;

import com.rentalplatform.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    @Query("SELECT r FROM ReviewEntity r WHERE r.listing.id = :listingId")
    Page<ReviewEntity> findAllByListingId(@Param("listingId") Long listingId, Pageable pageable);
}
