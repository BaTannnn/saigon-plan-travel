package com.saigonplantravel.backend.scheduling.entity;

import com.saigonplantravel.backend.place.scheduling.OpeningHoursStatus;
import com.saigonplantravel.backend.scheduling.domain.ItineraryWarningCode;
import com.saigonplantravel.backend.scheduling.domain.SchedulingAlgorithmVersion;
import com.saigonplantravel.backend.scheduling.domain.SchedulingPolicy;
import com.saigonplantravel.backend.scheduling.domain.algorithm.CandidateRejectionReason;
import com.saigonplantravel.backend.scheduling.domain.algorithm.GreedySchedulingOutcome;
import com.saigonplantravel.backend.scheduling.domain.algorithm.SchedulingInput;
import com.saigonplantravel.backend.scheduling.domain.scoring.EvaluatedCandidate;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.service.TripSchedulingSnapshot;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.TreeSet;

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

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "trip_id", nullable = false, updatable = false)
    private Long tripId;

    @Column(name = "trip_public_id_snapshot", nullable = false, updatable = false)
    private UUID tripPublicIdSnapshot;

    @Column(name = "trip_updated_at_snapshot", nullable = false, updatable = false)
    private OffsetDateTime tripUpdatedAtSnapshot;

    @Column(name = "trip_date", nullable = false, updatable = false)
    private LocalDate tripDate;

    @Column(name = "window_start", nullable = false, updatable = false)
    private LocalTime windowStart;

    @Column(name = "window_end", nullable = false, updatable = false)
    private LocalTime windowEnd;

    @Column(name = "origin_latitude", nullable = false, precision = 10, scale = 7, updatable = false)
    private BigDecimal originLatitude;

    @Column(name = "origin_longitude", nullable = false, precision = 10, scale = 7, updatable = false)
    private BigDecimal originLongitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "travel_pace", nullable = false, length = 20, updatable = false)
    private TravelPace travelPace;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_preference", nullable = false, length = 20, updatable = false)
    private EnvironmentPreference environmentPreference;

    @Column(name = "initial_budget", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal initialBudget;

    @Column(name = "total_estimated_cost", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal totalEstimatedCost;

    @Column(name = "remaining_budget", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal remainingBudget;

    @Column(name = "total_distance_km", nullable = false, precision = 10, scale = 3, updatable = false)
    private BigDecimal totalDistanceKm;

    @Column(name = "total_travel_minutes", nullable = false, updatable = false)
    private int totalTravelMinutes;

    @Column(name = "total_visit_minutes", nullable = false, updatable = false)
    private int totalVisitMinutes;

    @Column(name = "total_waiting_minutes", nullable = false, updatable = false)
    private int totalWaitingMinutes;

    @Column(name = "remaining_minutes", nullable = false, updatable = false)
    private int remainingMinutes;

    @Column(name = "candidate_count", nullable = false, updatable = false)
    private int candidateCount;

    @Column(name = "scheduled_count", nullable = false, updatable = false)
    private int scheduledCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm_version", nullable = false, length = 40, updatable = false)
    private SchedulingAlgorithmVersion algorithmVersion;

    @Column(name = "average_speed_kmh", nullable = false, precision = 6, scale = 2, updatable = false)
    private BigDecimal averageSpeedKmh;

    @Column(name = "fixed_transfer_minutes", nullable = false, updatable = false)
    private int fixedTransferMinutes;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private OffsetDateTime generatedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "itinerary_preferred_categories",
            joinColumns = @JoinColumn(name = "itinerary_id")
    )
    @Column(name = "category_id", nullable = false)
    private Set<Long> preferredCategoryIds = new HashSet<>();

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNo ASC")
    private List<ItineraryItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ItineraryWarning> warnings = new ArrayList<>();

    public static Itinerary create(
            Long userId,
            SchedulingInput input,
            GreedySchedulingOutcome outcome
    ) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (input == null || outcome == null) {
            throw new IllegalArgumentException("input and outcome must not be null");
        }
        if (outcome.scheduledCandidates().isEmpty()) {
            throw new IllegalArgumentException("an itinerary must contain at least one item");
        }
        if (outcome.candidatePoolSize() != input.candidates().size()) {
            throw new IllegalArgumentException("candidate pool size does not match scheduling input");
        }

        TripSchedulingSnapshot trip = input.trip();
        SchedulingPolicy policy = input.policy();
        Itinerary itinerary = new Itinerary();

        itinerary.publicId = UUID.randomUUID();
        itinerary.userId = userId;
        itinerary.tripId = trip.tripId();
        itinerary.tripPublicIdSnapshot = trip.publicId();
        itinerary.tripUpdatedAtSnapshot = trip.updatedAt();
        itinerary.tripDate = trip.tripDate();
        itinerary.windowStart = trip.startTime();
        itinerary.windowEnd = trip.endTime();
        itinerary.originLatitude = trip.startLatitude();
        itinerary.originLongitude = trip.startLongitude();
        itinerary.travelPace = trip.travelPace();
        itinerary.environmentPreference = trip.environmentPreference();
        itinerary.initialBudget = trip.budget().setScale(2, RoundingMode.HALF_UP);
        itinerary.candidateCount = outcome.candidatePoolSize();
        itinerary.scheduledCount = outcome.scheduledCandidates().size();
        itinerary.algorithmVersion = policy.algorithmVersion();
        itinerary.averageSpeedKmh = policy.averageSpeedKmh().setScale(2, RoundingMode.HALF_UP);
        itinerary.fixedTransferMinutes = policy.fixedTransferMinutes();
        itinerary.generatedAt = input.generatedAt();
        itinerary.preferredCategoryIds.addAll(trip.preferredCategoryIds());

        itinerary.addItems(outcome.scheduledCandidates());
        itinerary.calculateSummary(outcome);
        itinerary.addWarnings(outcome);
        itinerary.validateTimeline();

        return itinerary;
    }

    public Set<Long> getPreferredCategoryIds() {
        return Collections.unmodifiableSortedSet(
                new TreeSet<>(preferredCategoryIds)
        );
    }

    public List<ItineraryItem> getItems() {
        return List.copyOf(items);
    }

    public List<ItineraryWarning> getWarnings() {
        return List.copyOf(warnings);
    }

    private void addItems(List<EvaluatedCandidate> scheduledCandidates) {
        int sequence = 1;
        for (EvaluatedCandidate candidate : scheduledCandidates) {
            items.add(ItineraryItem.from(this, sequence, candidate));
            sequence++;
        }
    }

    private void calculateSummary(GreedySchedulingOutcome outcome) {
        totalEstimatedCost = items.stream()
                .map(ItineraryItem::getEstimatedCost)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        remainingBudget = initialBudget.subtract(totalEstimatedCost);
        totalDistanceKm = items.stream()
                .map(ItineraryItem::getDistanceKmFromPrevious)
                .reduce(BigDecimal.ZERO.setScale(3), BigDecimal::add);
        totalTravelMinutes = items.stream()
                .mapToInt(ItineraryItem::getTravelMinutesFromPrevious)
                .sum();
        totalVisitMinutes = items.stream()
                .mapToInt(ItineraryItem::getVisitMinutes)
                .sum();
        totalWaitingMinutes = items.stream()
                .mapToInt(ItineraryItem::getWaitingMinutes)
                .sum();
        remainingMinutes = Math.toIntExact(Duration.between(
                outcome.finalTime(),
                LocalDateTime.of(tripDate, windowEnd)
        ).toMinutes());

        if (remainingBudget.compareTo(outcome.remainingBudget()) != 0) {
            throw new IllegalArgumentException("stored remaining budget does not match scheduling outcome");
        }
        if (remainingMinutes < 0) {
            throw new IllegalArgumentException("scheduled items exceed the trip window");
        }
    }

    private void addWarnings(GreedySchedulingOutcome outcome) {
        int sortOrder = 0;
        sortOrder = addWarning(null, ItineraryWarningCode.TRAVEL_TIME_ESTIMATED, sortOrder);
        sortOrder = addWarning(null, ItineraryWarningCode.COST_USES_MINIMUM_ESTIMATE, sortOrder);

        boolean unusedSchedulableCandidate = outcome.terminalRejections().stream()
                .anyMatch(rejection -> rejection.reason() != CandidateRejectionReason.ENVIRONMENT);
        if (remainingMinutes >= 60 && unusedSchedulableCandidate) {
            sortOrder = addWarning(null, ItineraryWarningCode.UNUSED_TIME_REMAINS, sortOrder);
        }

        Set<Long> coveredCategoryIds = new LinkedHashSet<>();
        for (EvaluatedCandidate candidate : outcome.scheduledCandidates()) {
            coveredCategoryIds.addAll(
                    candidate.candidate().matchedPreferredCategoryIds()
            );
        }
        if (!coveredCategoryIds.containsAll(preferredCategoryIds)) {
            sortOrder = addWarning(null, ItineraryWarningCode.LIMITED_CATEGORY_COVERAGE, sortOrder);
        }

        for (ItineraryItem item : items) {
            if (item.getOpeningHoursStatus() == OpeningHoursStatus.UNKNOWN) {
                sortOrder = addWarning(
                        item.getSequenceNo(),
                        ItineraryWarningCode.OPENING_HOURS_UNKNOWN,
                        sortOrder
                );
            }
        }
    }

    private int addWarning(
            Integer itemSequence,
            ItineraryWarningCode code,
            int sortOrder
    ) {
        warnings.add(new ItineraryWarning(
                this,
                itemSequence,
                code,
                code.message(),
                sortOrder
        ));
        return sortOrder + 1;
    }

    private void validateTimeline() {
        LocalDateTime previousEnd = LocalDateTime.of(tripDate, windowStart);
        Set<Long> placeIds = new HashSet<>();

        for (int index = 0; index < items.size(); index++) {
            ItineraryItem item = items.get(index);
            if (item.getSequenceNo() != index + 1) {
                throw new IllegalArgumentException("item sequence must be continuous from one");
            }
            if (!placeIds.add(item.getPlaceId())) {
                throw new IllegalArgumentException("an itinerary cannot repeat a place");
            }
            if (!item.getDepartureAt(tripDate).equals(previousEnd)) {
                throw new IllegalArgumentException("item timeline does not continue from the previous item");
            }
            previousEnd = item.getVisitEndAt(tripDate);
        }
    }
}
