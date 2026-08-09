package com.saigonplantravel.backend.scheduling.entity;

import com.saigonplantravel.backend.scheduling.domain.SchedulingAlgorithmVersion;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "itineraries")
public class Itinerary {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "public_id",
            nullable = false,
            unique = true
    )
    private UUID publicId;

    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    @Column(
            name = "trip_id",
            nullable = false
    )
    private Long tripId;

    @Column(
            name = "trip_public_id_snapshot",
            nullable = false
    )
    private UUID tripPublicIdSnapshot;

    @Column(
            name = "trip_updated_at_snapshot",
            nullable = false
    )
    private OffsetDateTime tripUpdatedAtSnapshot;

    @Column(
            name = "trip_date",
            nullable = false
    )
    private LocalDate tripDate;

    @Column(
            name = "window_start",
            nullable = false
    )
    private LocalTime windowStart;

    @Column(
            name = "window_end",
            nullable = false
    )
    private LocalTime windowEnd;

    @Column(
            name = "origin_latitude",
            nullable = false,
            precision = 10,
            scale = 7
    )
    private BigDecimal originLatitude;

    @Column(
            name = "origin_longitude",
            nullable = false,
            precision = 10,
            scale = 7
    )
    private BigDecimal originLongitude;

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

    @Column(
            name = "initial_budget",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal initialBudget;

    @Column(
            name = "total_estimated_cost",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal totalEstimatedCost;

    @Column(
            name = "remaining_budget",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal remainingBudget;

    @Column(
            name = "total_distance_km",
            nullable = false,
            precision = 10,
            scale = 3
    )
    private BigDecimal totalDistanceKm;

    @Column(
            name = "total_travel_minutes",
            nullable = false
    )
    private int totalTravelMinutes;

    @Column(
            name = "total_visit_minutes",
            nullable = false
    )
    private int totalVisitMinutes;

    @Column(
            name = "total_waiting_minutes",
            nullable = false
    )
    private int totalWaitingMinutes;

    @Column(
            name = "remaining_minutes",
            nullable = false
    )
    private int remainingMinutes;

    @Column(
            name = "candidate_count",
            nullable = false
    )
    private int candidateCount;

    @Column(
            name = "scheduled_count",
            nullable = false
    )
    private int scheduledCount;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "algorithm_version",
            nullable = false,
            length = 40
    )
    private SchedulingAlgorithmVersion algorithmVersion;

    @Column(
            name = "average_speed_kmh",
            nullable = false,
            precision = 6,
            scale = 2
    )
    private BigDecimal averageSpeedKmh;

    @Column(
            name = "fixed_transfer_minutes",
            nullable = false
    )
    private int fixedTransferMinutes;

    @Column(
            name = "generated_at",
            nullable = false
    )
    private OffsetDateTime generatedAt;

    @ElementCollection(
            fetch = FetchType.LAZY
    )
    @CollectionTable(
            name = "itinerary_preferred_categories",
            joinColumns = @JoinColumn(
                    name = "itinerary_id"
            )
    )
    @Column(
            name = "category_id",
            nullable = false
    )
    private Set<Long> preferredCategoryIds =
            new HashSet<>();

    protected Itinerary() {
    }
}