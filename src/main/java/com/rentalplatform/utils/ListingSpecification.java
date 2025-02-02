package com.rentalplatform.utils;

import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.ListingType;
import com.rentalplatform.entity.ReviewEntity;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ListingSpecification {

    public static Specification<ListingEntity> hasTitle(String title) {
        if(title != null && !title.isEmpty()) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("title"), "%" + title + "%");
        }
        return null;
    }

    public static Specification<ListingEntity> hasAddress(String address) {
        if(address != null && !address.isEmpty()) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("address"), "%" + address + "%");
        }
        return null;
    }

    public static Specification<ListingEntity> hasMinPrice(Double minPrice) {
        if(minPrice != null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
        }
        return null;
    }

    public static Specification<ListingEntity> hasMaxPrice(Double maxPrice) {
        if(maxPrice != null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
        }
        return null;
    }

    public static Specification<ListingEntity> hasNumberOfRooms(Integer numberOfRooms) {
        if(numberOfRooms != null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("numberOfRooms"), numberOfRooms);
        }
        return null;
    }

    public static Specification<ListingEntity> hasType(ListingType type) {
        if(type != null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"), type);
        }
        return null;
    }

    public static Specification<ListingEntity> hasAvailableFrom(LocalDate availableFrom) {
        if(availableFrom != null) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("nextAvailableDate"),availableFrom);
        }
        return null;
    }

    public static Specification<ListingEntity> hasMinAverageRating(Double minAverageRating) {
        return (root, query, criteriaBuilder) -> {
            if(minAverageRating == null) return null;

            Subquery<Double> averageRatingSubquery = query.subquery(Double.class);
            Root<ReviewEntity> reviewRoot = averageRatingSubquery.from(ReviewEntity.class);

            averageRatingSubquery.select(criteriaBuilder.avg(reviewRoot.get("rating")))
                    .where(criteriaBuilder.equal(reviewRoot.get("listing"), root));
            return criteriaBuilder.greaterThanOrEqualTo(averageRatingSubquery, minAverageRating);
        };
    }
}
