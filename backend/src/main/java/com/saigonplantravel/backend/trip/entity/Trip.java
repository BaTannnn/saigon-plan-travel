package com.saigonplantravel.backend.trip.entity;

import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "public_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID publicId;

    @Column(
            name = "user_id",
            nullable = false,
            updatable = false
    )
    private Long userId;

    @Column(
            name = "trip_date",
            nullable = false
    )
    private LocalDate tripDate;

    @Column(
            name = "start_time",
            nullable = false
    )
    private LocalTime startTime;

    @Column(
            name = "end_time",
            nullable = false
    )
    private LocalTime endTime;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal budget;

    @Column(
            name = "start_location_label",
            nullable = false,
            length = 255
    )
    private String startLocationLabel;

    @Column(
            name = "start_latitude",
            nullable = false,
            precision = 10,
            scale = 7
    )
    private BigDecimal startLatitude;

    @Column(
            name = "start_longitude",
            nullable = false,
            precision = 10,
            scale = 7
    )
    private BigDecimal startLongitude;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "travel_pace",
            nullable = false,
            length = 20
    )
    private TravelPace travelPace;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "environment_preference",
            nullable = false,
            length = 20
    )
    private EnvironmentPreference environmentPreference;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "trip_category_preferences",
            joinColumns = @JoinColumn(name = "trip_id")
    )
    @Column(
            name = "category_id",
            nullable = false
    )
    private Set<Long> preferredCategoryIds = new HashSet<>();

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    public Trip(
            Long userId,
            LocalDate tripDate,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal budget,
            String startLocationLabel,
            BigDecimal startLatitude,
            BigDecimal startLongitude,
            TravelPace travelPace,
            EnvironmentPreference environmentPreference,
            Set<Long> preferredCategoryIds,
            OffsetDateTime createdAt
    ) {
        this.publicId = UUID.randomUUID();
        this.userId = userId;
        this.tripDate = tripDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.budget = budget;
        this.startLocationLabel = startLocationLabel.trim();
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.travelPace = travelPace;
        this.environmentPreference = environmentPreference;
        this.preferredCategoryIds =
                new HashSet<>(preferredCategoryIds);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void replaceDraft(
            LocalDate tripDate,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal budget,
            String startLocationLabel,
            BigDecimal startLatitude,
            BigDecimal startLongitude,
            TravelPace travelPace,
            EnvironmentPreference environmentPreference,
            Set<Long> preferredCategoryIds,
            OffsetDateTime updatedAt
    ) {
        this.tripDate = tripDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.budget = budget;
        this.startLocationLabel = startLocationLabel.trim();
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.travelPace = travelPace;
        this.environmentPreference = environmentPreference;

        this.preferredCategoryIds.clear();
        this.preferredCategoryIds.addAll(
                preferredCategoryIds
        );

        this.updatedAt = updatedAt;
    }

    public Set<Long> getPreferredCategoryIds() {
        return Set.copyOf(preferredCategoryIds);
    }
}