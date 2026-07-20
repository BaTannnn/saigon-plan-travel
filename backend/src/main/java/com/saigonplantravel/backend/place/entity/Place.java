package com.saigonplantravel.backend.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "full_description")
    private String fullDescription;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "estimated_visit_minutes", nullable = false)
    private Integer estimatedVisitMinutes;

    @Column(name = "min_cost", nullable = false)
    private BigDecimal minCost;

    @Column(name = "max_cost", nullable = false)
    private BigDecimal maxCost;

    @Column(nullable = false)
    private Boolean indoor;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Place(
            String name,
            String slug,
            String address,
            String district,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer estimatedVisitMinutes,
            BigDecimal minCost,
            BigDecimal maxCost,
            Boolean indoor
    ) {
        this.name = name;
        this.slug = slug;
        this.address = address;
        this.district = district;
        this.latitude = latitude;
        this.longitude = longitude;
        this.estimatedVisitMinutes = estimatedVisitMinutes;
        this.minCost = minCost;
        this.maxCost = maxCost;
        this.indoor = indoor;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void updateBasicInformation(
            String name,
            String shortDescription,
            String address
    ) {
        this.name = name;
        this.shortDescription = shortDescription;
        this.address = address;
    }
}
