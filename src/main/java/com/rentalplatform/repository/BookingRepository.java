package com.rentalplatform.repository;

import com.rentalplatform.entity.BookingEntity;
import com.rentalplatform.entity.BookingStatus;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.tenant.username = :username OR b.listing.landlord.username = :username")
    Page<BookingEntity> findAllByTenantUsernameOrLandlordUsername(@Param("username") String username, Pageable pageable);

    @Query("SELECT b FROM BookingEntity b WHERE b.listing.landlord.username = :username")
    Page<BookingEntity> findAllByListingLandlordUsername(@Param("username") String username, Pageable pageable);

    @Query("SELECT MAX(b.endDate) FROM BookingEntity b WHERE b.listing.id = :listingId AND b.status = :status")
    Optional<Instant> findMaxEndDateByListingIdAndStatus(@Param("listingId") Long listingId,
                                                         @Param("status") BookingStatus status);

    List<BookingEntity> findAllByStatusAndEndDateBefore(BookingStatus status, Instant now);

    boolean existsByListingAndTenantAndStatus(ListingEntity listing, UserEntity tenant, BookingStatus status);
}
