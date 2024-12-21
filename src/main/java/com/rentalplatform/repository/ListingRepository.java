package com.rentalplatform.repository;

import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<ListingEntity, Long>, JpaSpecificationExecutor<ListingEntity> {
    List<ListingEntity> findAllByLandlord(UserEntity landlord);
    boolean existsByTitleAndAddressAndLandlord(String title, String address, UserEntity landlord);
}
