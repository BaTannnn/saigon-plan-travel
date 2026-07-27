package com.saigonplantravel.backend.place.entity;

import com.saigonplantravel.backend.place.domain.AdministrativeUnitType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

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

    @Column(name = "administrative_unit_name", length = 100)
    private String administrativeUnitName;

    @Enumerated(EnumType.STRING)
    @Column(name = "administrative_unit_type", length = 20)
    private AdministrativeUnitType administrativeUnitType;

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

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "place_categories",
            joinColumns = @JoinColumn(name = "place_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "place", fetch = LAZY)
    private List<OpeningHour> openingHours = new ArrayList<>();

    public Place(
            String name,
            String slug,
            String address,
            String administrativeUnitName,
            AdministrativeUnitType administrativeUnitType,
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
        this.administrativeUnitName = administrativeUnitName;
        this.administrativeUnitType = administrativeUnitType;
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
