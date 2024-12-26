package com.rentalplatform.repository;

import com.rentalplatform.entity.BookingEntity;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findAllByListing(ListingEntity listing);
}
