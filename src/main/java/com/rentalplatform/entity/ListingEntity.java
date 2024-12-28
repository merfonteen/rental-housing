package com.rentalplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "listings",
        indexes = {
                @Index(name = "idx_listing_title", columnList = "title"),
                @Index(name = "idx_listing_address", columnList = "address"),
                @Index(name = "idx_listing_price", columnList = "price"),
                @Index(name = "idx_listing_number_of_rooms", columnList = "number_of_rooms"),
                @Index(name = "idx_listing_type", columnList = "type"),
                @Index(name = "idx_listing_landlord_id", columnList = "landlord_id")
        })
public class ListingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private Double price;

    private String address;

    @Column(name = "number_of_rooms")
    private Integer numberOfRooms;

    @Enumerated(EnumType.STRING)
    private ListingType type;

    @ManyToOne
    @JoinColumn(name = "landlord_id")
    private UserEntity landlord;

    @Builder.Default
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewEntity> reviews = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingEntity> bookings = new ArrayList<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListingEntity that = (ListingEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
