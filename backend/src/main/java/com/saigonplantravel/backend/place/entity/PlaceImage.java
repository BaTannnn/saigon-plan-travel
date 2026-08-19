package com.saigonplantravel.backend.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "place_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false, updatable = false)
    private Place place;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "is_primary", nullable = false)
    private Boolean primary;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public PlaceImage(Place place, String storageKey, String url, String altText) {
        this.place = place;
        this.storageKey = storageKey;
        this.url = url;
        this.altText = altText;
        this.primary = true;
        this.sortOrder = 0;
    }
}
