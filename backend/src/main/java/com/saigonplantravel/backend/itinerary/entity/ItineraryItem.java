package com.saigonplantravel.backend.itinerary.entity;

import com.saigonplantravel.backend.place.entity.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "itinerary_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id", nullable = false, updatable = false)
    private Itinerary itinerary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    ItineraryItem(Itinerary itinerary, Place place, Integer sequenceNo, OffsetDateTime createdAt) {
        this.publicId = UUID.randomUUID();
        this.itinerary = itinerary;
        this.place = place;
        this.sequenceNo = sequenceNo;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    void replacePlace(Place replacementPlace, OffsetDateTime updatedAt) {
        this.place = replacementPlace;
        this.updatedAt = updatedAt;
    }

    void changeSequence(Integer sequenceNo, OffsetDateTime updatedAt) {
        if (!this.sequenceNo.equals(sequenceNo)) {
            this.sequenceNo = sequenceNo;
            this.updatedAt = updatedAt;
        }
    }
}
