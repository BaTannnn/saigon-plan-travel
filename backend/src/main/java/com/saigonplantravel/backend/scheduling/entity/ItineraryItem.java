package com.saigonplantravel.backend.scheduling.entity;

import com.saigonplantravel.backend.place.domain.AdministrativeUnitType;
import com.saigonplantravel.backend.place.scheduling.OpeningHoursSnapshot;
import com.saigonplantravel.backend.place.scheduling.OpeningHoursStatus;
import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;
import com.saigonplantravel.backend.scheduling.domain.scoring.EvaluatedCandidate;
import com.saigonplantravel.backend.scheduling.domain.visit.VisitSchedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "itinerary_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id", nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private Itinerary itinerary;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private int sequenceNo;

    @Column(name = "place_id", nullable = false, updatable = false)
    private Long placeId;

    @Column(name = "place_name", nullable = false, length = 150, updatable = false)
    private String placeName;

    @Column(name = "place_slug", nullable = false, length = 180, updatable = false)
    private String placeSlug;

    @Column(name = "place_address", nullable = false, length = 255, updatable = false)
    private String placeAddress;

    @Column(name = "place_administrative_unit_name", length = 150, updatable = false)
    private String placeAdministrativeUnitName;

    @Enumerated(EnumType.STRING)
    @Column(name = "place_administrative_unit_type", length = 30, updatable = false)
    private AdministrativeUnitType placeAdministrativeUnitType;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7, updatable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7, updatable = false)
    private BigDecimal longitude;

    @Column(name = "indoor", nullable = false, updatable = false)
    private boolean indoor;

    @Column(name = "base_visit_minutes", nullable = false, updatable = false)
    private int baseVisitMinutes;

    @Column(name = "travel_minutes_from_previous", nullable = false, updatable = false)
    private int travelMinutesFromPrevious;

    @Column(name = "distance_km_from_previous", nullable = false, precision = 10, scale = 3, updatable = false)
    private BigDecimal distanceKmFromPrevious;

    @Column(name = "arrival_time", nullable = false, updatable = false)
    private LocalTime arrivalTime;

    @Column(name = "waiting_minutes", nullable = false, updatable = false)
    private int waitingMinutes;

    @Column(name = "visit_start", nullable = false, updatable = false)
    private LocalTime visitStart;

    @Column(name = "visit_end", nullable = false, updatable = false)
    private LocalTime visitEnd;

    @Column(name = "visit_minutes", nullable = false, updatable = false)
    private int visitMinutes;

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal estimatedCost;

    @Column(name = "selection_score", nullable = false, precision = 10, scale = 4, updatable = false)
    private BigDecimal selectionScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "opening_hours_status", nullable = false, length = 20, updatable = false)
    private OpeningHoursStatus openingHoursStatus;

    @Column(name = "opening_time", updatable = false)
    private LocalTime openingTime;

    @Column(name = "closing_time", updatable = false)
    private LocalTime closingTime;

    static ItineraryItem from(
            Itinerary itinerary,
            int sequenceNo,
            EvaluatedCandidate evaluated
    ) {
        PlaceSchedulingCandidate candidate = evaluated.candidate();
        VisitSchedule schedule = evaluated.visitSchedule();
        OpeningHoursSnapshot hours = candidate.openingHours();

        if (hours.status() == OpeningHoursStatus.CLOSED) {
            throw new IllegalArgumentException("a closed place cannot be persisted as an itinerary item");
        }

        ItineraryItem item = new ItineraryItem();
        item.itinerary = itinerary;
        item.sequenceNo = sequenceNo;
        item.placeId = candidate.placeId();
        item.placeName = candidate.name();
        item.placeSlug = candidate.slug();
        item.placeAddress = candidate.address();
        item.placeAdministrativeUnitName = candidate.administrativeUnitName();
        item.placeAdministrativeUnitType = candidate.administrativeUnitType();
        item.latitude = candidate.latitude();
        item.longitude = candidate.longitude();
        item.indoor = candidate.indoor();
        item.baseVisitMinutes = candidate.baseVisitMinutes();
        item.travelMinutesFromPrevious = evaluated.travelEstimate().travelMinutes();
        item.distanceKmFromPrevious = evaluated.travelEstimate()
                .distanceKilometers()
                .setScale(3, RoundingMode.HALF_UP);
        item.arrivalTime = schedule.arrivalAt().toLocalTime();
        item.waitingMinutes = Math.toIntExact(schedule.waitingMinutes());
        item.visitStart = schedule.visitStartAt().toLocalTime();
        item.visitEnd = schedule.visitEndAt().toLocalTime();
        item.visitMinutes = evaluated.adjustedVisitMinutes();
        item.estimatedCost = candidate.minCost().setScale(2, RoundingMode.HALF_UP);
        item.selectionScore = evaluated.score().totalScore().setScale(4, RoundingMode.HALF_UP);
        item.openingHoursStatus = hours.status();
        item.openingTime = hours.openTime();
        item.closingTime = hours.closeTime();
        return item;
    }

    LocalDateTime getDepartureAt(LocalDate tripDate) {
        return LocalDateTime.of(tripDate, arrivalTime)
                .minusMinutes(travelMinutesFromPrevious);
    }

    LocalDateTime getVisitEndAt(LocalDate tripDate) {
        return LocalDateTime.of(tripDate, visitEnd);
    }
}
