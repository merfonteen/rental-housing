package com.rentalplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "bookings",
        indexes = {
                @Index(name = "idx_review_listing_id", columnList = "listing_id"),
                @Index(name = "idx_review_tenant_id", columnList = "tenant_id")
        })
public class BookingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "listing_id")
    private ListingEntity listing;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private UserEntity tenant;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingEntity that = (BookingEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
