package com.rentalplatform.repository;

import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingRepository extends JpaRepository<ListingEntity, Long>, JpaSpecificationExecutor<ListingEntity> {
    @EntityGraph(attributePaths = {"reviews"})
    @Query("SELECT l FROM ListingEntity l WHERE l.landlord.id = :landlordId")
    Page<ListingEntity> findAllWithReviewsByLandlordId(@Param("landlordId") Long landlordId, Pageable pageable);

    @EntityGraph(attributePaths = {"reviews"})
    @Override
    Page<ListingEntity> findAll(Specification<ListingEntity> spec, Pageable pageable);

    boolean existsByTitleAndAddressAndLandlord(String title, String address, UserEntity landlord);
}
