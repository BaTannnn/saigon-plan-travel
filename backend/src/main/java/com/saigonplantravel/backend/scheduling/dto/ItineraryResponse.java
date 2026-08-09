package com.saigonplantravel.backend.scheduling.dto;

import com.saigonplantravel.backend.place.domain.AdministrativeUnitType;
import com.saigonplantravel.backend.place.scheduling.OpeningHoursStatus;
import com.saigonplantravel.backend.scheduling.domain.ItineraryWarningCode;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ItineraryResponse(
        UUID publicId,
        UUID tripPublicId,
        String algorithmVersion,
        OffsetDateTime generatedAt,
        boolean stale,
        SchedulingAssumptions assumptions,
        TripWindow tripWindow,
        PreferencesSnapshot preferencesSnapshot,
        Coordinate origin,
        List<Item> items,
        Summary summary,
        List<Warning> warnings
) {

    public record SchedulingAssumptions(
            String travelEstimator,
            BigDecimal averageSpeedKmh,
            int fixedTransferMinutes,
            String costBasis,
            boolean returnToOrigin
    ) {
    }

    public record TripWindow(
            LocalDate tripDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }

    public record PreferencesSnapshot(
            TravelPace travelPace,
            EnvironmentPreference environmentPreference,
            Set<Long> preferredCategoryIds
    ) {
    }

    public record Coordinate(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }

    public record Item(
            int sequence,
            PlaceSnapshot place,
            TravelSnapshot travel,
            LocalTime arrivalTime,
            int waitingMinutes,
            LocalTime visitStart,
            LocalTime visitEnd,
            int baseVisitMinutes,
            int visitMinutes,
            BigDecimal estimatedCost,
            BigDecimal selectionScore,
            OpeningHours openingHours
    ) {
    }

    public record PlaceSnapshot(
            Long id,
            String name,
            String slug,
            String address,
            String administrativeUnitName,
            AdministrativeUnitType administrativeUnitType,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean indoor
    ) {
    }

    public record TravelSnapshot(
            int estimatedMinutes,
            BigDecimal straightLineDistanceKm
    ) {
    }

    public record OpeningHours(
            OpeningHoursStatus status,
            LocalTime openTime,
            LocalTime closeTime
    ) {
    }

    public record Summary(
            int candidateCount,
            int scheduledCount,
            BigDecimal totalStraightLineDistanceKm,
            BigDecimal totalEstimatedCost,
            BigDecimal remainingBudget,
            int totalTravelMinutes,
            int totalVisitMinutes,
            int totalWaitingMinutes,
            int remainingMinutes
    ) {
    }

    public record Warning(
            ItineraryWarningCode code,
            String message,
            Integer itemSequence
    ) {
    }
}
