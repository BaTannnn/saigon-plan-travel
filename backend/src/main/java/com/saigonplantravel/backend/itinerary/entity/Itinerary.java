package com.saigonplantravel.backend.itinerary.entity;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.trip.entity.Trip;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "itineraries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false, unique = true, updatable = false)
    private Trip trip;
    
    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequenceNo ASC")
    private List<ItineraryItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Itinerary(Trip trip, OffsetDateTime createdAt) {
        this.publicId = UUID.randomUUID();
        this.trip = trip;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public ItineraryItem appendItem(Place place, OffsetDateTime createdAt) {
        ItineraryItem item = new ItineraryItem(this, place, items.size() + 1, createdAt);

        items.add(item);
        updatedAt = createdAt;

        return item;
    }

    public Optional<ItineraryItem> findItem(UUID itemPublicId) {
        return items.stream()
                .filter(item -> item.getPublicId().equals(itemPublicId))
                .findFirst();
    }

    public boolean containsPlace(Long placeId) {
        return items.stream().anyMatch(item -> item.getPlace().getId().equals(placeId));
    }

    public boolean containsOtherPlace(Long placeId, UUID currentItemPublicId) {
        return items.stream()
                .filter(item -> !item.getPublicId().equals(currentItemPublicId))
                .anyMatch(item -> item.getPlace().getId().equals(placeId));
    }

    public void removeItem(ItineraryItem item, OffsetDateTime updatedAt) {
        items.remove(item);
        this.updatedAt = updatedAt;
    }

    public void resequenceItems(OffsetDateTime updatedAt) {
        items.sort(Comparator.comparing(ItineraryItem::getSequenceNo));

        for (int index = 0; index < items.size(); index++) {
            items.get(index).changeSequence(index + 1, updatedAt);
        }
    }

    public void replaceItemPlace(ItineraryItem item, Place replacementPlace, OffsetDateTime updatedAt) {
        item.replacePlace(replacementPlace, updatedAt);
        this.updatedAt = updatedAt;
    }

    public List<ItineraryItem> getItems() {
        return List.copyOf(items);
    }
}
