package com.saigonplantravel.backend.place.entity;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

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
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "place", fetch = LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<OpeningHour> openingHours = new ArrayList<>();

    @OneToOne(mappedBy = "place", fetch = LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private PlaceImage coverImage;

    public Place(
            String name,
            String slug,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer estimatedVisitMinutes,
            BigDecimal minCost,
            BigDecimal maxCost,
            Boolean indoor) {
        this.name = name;
        this.slug = slug;
        this.address = address;
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
            String fullDescription,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer estimatedVisitMinutes,
            BigDecimal minCost,
            BigDecimal maxCost,
            Boolean indoor) {
        this.name = name;
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.estimatedVisitMinutes = estimatedVisitMinutes;
        this.minCost = minCost;
        this.maxCost = maxCost;
        this.indoor = indoor;
    }

    public void updateDescriptions(String shortDescription, String fullDescription) {
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
    }

    public void replaceCategories(Collection<Category> categories) {
        this.categories.clear();
        this.categories.addAll(categories);
    }

    public String getPrimaryImageUrl() {
        return coverImage == null ? null : coverImage.getUrl();
    }

    public String getPrimaryImageStorageKey() {
        return coverImage == null ? null : coverImage.getStorageKey();
    }

    public void replaceCoverImage(PlaceImage coverImage) {
        this.coverImage = coverImage;
    }

    public void removeCoverImage() {
        coverImage = null;
    }

    public void markClosed(Short dayOfWeek) {
        openingHour(dayOfWeek).markClosed();
    }

    public void markOpen(Short dayOfWeek, LocalTime openTime, LocalTime closeTime) {
        openingHour(dayOfWeek).markOpen(openTime, closeTime);
    }

    public void removeOpeningHour(Short dayOfWeek) {
        openingHours.removeIf(openingHour -> openingHour.getDayOfWeek().equals(dayOfWeek));
    }

    private OpeningHour openingHour(Short dayOfWeek) {
        return openingHours.stream()
                .filter(openingHour -> openingHour.getDayOfWeek().equals(dayOfWeek))
                .findFirst()
                .orElseGet(() -> {
                    OpeningHour openingHour = new OpeningHour(this, dayOfWeek);
                    openingHours.add(openingHour);
                    return openingHour;
                });
    }
}
