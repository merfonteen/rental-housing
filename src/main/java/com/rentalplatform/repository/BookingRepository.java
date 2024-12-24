package com.rentalplatform.repository;

import com.rentalplatform.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.tenant.username = :username OR b.listing.landlord.username = :username")
    List<BookingEntity> findAllByTenantUsernameOrLandlordUsername(@Param("username") String username);

    @Query("SELECT b FROM BookingEntity b " +
    "WHERE b.listing.landlord.username = :username")
    List<BookingEntity> findAllByListingLandlordUsername(String username);
}
