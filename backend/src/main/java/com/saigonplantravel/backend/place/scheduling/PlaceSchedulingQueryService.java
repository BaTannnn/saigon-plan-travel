package com.saigonplantravel.backend.place.scheduling;

import com.saigonplantravel.backend.place.repository.PlaceRepository;
import com.saigonplantravel.backend.place.repository.projection.PlaceSchedulingBaseRow;
import com.saigonplantravel.backend.place.repository.projection.PlaceSchedulingCategoryRow;
import com.saigonplantravel.backend.place.repository.projection.PlaceSchedulingOpeningHourRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Service
public class PlaceSchedulingQueryService
        implements PlaceSchedulingQuery {

    private final PlaceRepository placeRepository;

    public PlaceSchedulingQueryService(
            PlaceRepository placeRepository
    ) {
        this.placeRepository =
                Objects.requireNonNull(
                        placeRepository,
                        "placeRepository must not be null"
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlaceSchedulingCandidate> findCandidates(
            Set<Long> preferredCategoryIds,
            LocalDate tripDate
    ) {
        Set<Long> normalizedCategoryIds =
                normalizePreferredCategoryIds(
                        preferredCategoryIds
                );

        Objects.requireNonNull(
                tripDate,
                "tripDate must not be null"
        );

        if (normalizedCategoryIds.isEmpty()) {
            return List.of();
        }

        List<PlaceSchedulingBaseRow> baseRows =
                placeRepository
                        .findSchedulingBaseRowsByPreferredCategoryIds(
                                normalizedCategoryIds
                        );

        if (baseRows.isEmpty()) {
            return List.of();
        }

        Set<Long> placeIds =
                extractPlaceIds(baseRows);

        List<PlaceSchedulingCategoryRow> categoryRows =
                placeRepository
                        .findSchedulingCategoryRowsByPreferredCategoryIds(
                                normalizedCategoryIds
                        );

        short isoDayOfWeek =
                (short) tripDate
                        .getDayOfWeek()
                        .getValue();

        List<PlaceSchedulingOpeningHourRow> openingHourRows =
                placeRepository
                        .findSchedulingOpeningHourRows(
                                placeIds,
                                isoDayOfWeek
                        );

        Map<Long, Set<Long>> categoryIdsByPlaceId =
                groupCategoryIdsByPlaceId(
                        categoryRows,
                        placeIds
                );

        Map<Long, PlaceSchedulingOpeningHourRow>
                openingHourByPlaceId =
                indexOpeningHoursByPlaceId(
                        openingHourRows,
                        placeIds
                );

        return baseRows.stream()
                .sorted(
                        Comparator.comparing(
                                PlaceSchedulingBaseRow::getPlaceId
                        )
                )
                .map(
                        row -> mapCandidate(
                                row,
                                categoryIdsByPlaceId,
                                openingHourByPlaceId
                        )
                )
                .toList();
    }

    private Set<Long> normalizePreferredCategoryIds(
            Set<Long> preferredCategoryIds
    ) {
        Objects.requireNonNull(
                preferredCategoryIds,
                "preferredCategoryIds must not be null"
        );

        TreeSet<Long> normalizedIds =
                new TreeSet<>();

        for (Long categoryId : preferredCategoryIds) {
            if (categoryId == null
                    || categoryId <= 0) {
                throw new IllegalArgumentException(
                        "preferred category IDs must be positive"
                );
            }

            normalizedIds.add(categoryId);
        }

        return Set.copyOf(normalizedIds);
    }

    private Set<Long> extractPlaceIds(
            List<PlaceSchedulingBaseRow> baseRows
    ) {
        Set<Long> placeIds =
                new LinkedHashSet<>();

        for (PlaceSchedulingBaseRow row : baseRows) {
            Long placeId =
                    requireProjectionValue(
                            row.getPlaceId(),
                            "placeId"
                    );

            placeIds.add(placeId);
        }

        return Set.copyOf(placeIds);
    }

    private Map<Long, Set<Long>>
    groupCategoryIdsByPlaceId(
            List<PlaceSchedulingCategoryRow> categoryRows,
            Set<Long> expectedPlaceIds
    ) {
        Map<Long, Set<Long>> groupedCategoryIds =
                new HashMap<>();

        for (PlaceSchedulingCategoryRow row : categoryRows) {
            Long placeId =
                    requireProjectionValue(
                            row.getPlaceId(),
                            "categoryRow.placeId"
                    );

            Long categoryId =
                    requireProjectionValue(
                            row.getCategoryId(),
                            "categoryRow.categoryId"
                    );

            if (!expectedPlaceIds.contains(placeId)) {
                throw new IllegalStateException(
                        "Category row references an unexpected placeId="
                                + placeId
                );
            }

            groupedCategoryIds
                    .computeIfAbsent(
                            placeId,
                            ignored -> new TreeSet<>()
                    )
                    .add(categoryId);
        }

        return groupedCategoryIds;
    }

    private Map<Long, PlaceSchedulingOpeningHourRow>
    indexOpeningHoursByPlaceId(
            List<PlaceSchedulingOpeningHourRow> openingHourRows,
            Set<Long> expectedPlaceIds
    ) {
        Map<Long, PlaceSchedulingOpeningHourRow>
                rowsByPlaceId =
                new HashMap<>();

        for (PlaceSchedulingOpeningHourRow row
                : openingHourRows) {
            Long placeId =
                    requireProjectionValue(
                            row.getPlaceId(),
                            "openingHourRow.placeId"
                    );

            if (!expectedPlaceIds.contains(placeId)) {
                throw new IllegalStateException(
                        "Opening-hour row references an unexpected placeId="
                                + placeId
                );
            }

            PlaceSchedulingOpeningHourRow previous =
                    rowsByPlaceId.putIfAbsent(
                            placeId,
                            row
                    );

            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate opening-hour rows for placeId="
                                + placeId
                );
            }
        }

        return rowsByPlaceId;
    }

    private PlaceSchedulingCandidate mapCandidate(
            PlaceSchedulingBaseRow row,
            Map<Long, Set<Long>> categoryIdsByPlaceId,
            Map<Long, PlaceSchedulingOpeningHourRow>
                    openingHourByPlaceId
    ) {
        Long placeId =
                requireProjectionValue(
                        row.getPlaceId(),
                        "baseRow.placeId"
                );

        Set<Long> matchedCategoryIds =
                categoryIdsByPlaceId.get(placeId);

        if (matchedCategoryIds == null
                || matchedCategoryIds.isEmpty()) {
            throw new IllegalStateException(
                    "Scheduling place has no matched preferred category: placeId="
                            + placeId
            );
        }

        Integer baseVisitMinutes =
                requireProjectionValue(
                        row.getBaseVisitMinutes(),
                        "baseRow.baseVisitMinutes"
                );

        Boolean indoor =
                requireProjectionValue(
                        row.getIndoor(),
                        "baseRow.indoor"
                );

        OpeningHoursSnapshot openingHours =
                mapOpeningHours(
                        openingHourByPlaceId.get(placeId),
                        placeId
                );

        return new PlaceSchedulingCandidate(
                placeId,
                row.getName(),
                row.getSlug(),
                row.getAddress(),
                row.getAdministrativeUnitName(),
                row.getAdministrativeUnitType(),
                row.getLatitude(),
                row.getLongitude(),
                baseVisitMinutes,
                row.getMinCost(),
                indoor,
                matchedCategoryIds,
                openingHours
        );
    }

    private OpeningHoursSnapshot mapOpeningHours(
            PlaceSchedulingOpeningHourRow row,
            Long placeId
    ) {
        if (row == null) {
            return OpeningHoursSnapshot.unknown();
        }

        Boolean closed =
                requireProjectionValue(
                        row.getClosed(),
                        "openingHourRow.closed"
                );

        LocalTime openTime =
                row.getOpenTime();

        LocalTime closeTime =
                row.getCloseTime();

        if (closed) {
            if (openTime != null
                    || closeTime != null) {
                throw new IllegalStateException(
                        "Closed opening-hour row must not contain an interval: placeId="
                                + placeId
                );
            }

            return OpeningHoursSnapshot.closed();
        }

        if (openTime == null
                || closeTime == null) {
            throw new IllegalStateException(
                    "Open opening-hour row must contain an interval: placeId="
                            + placeId
            );
        }

        return OpeningHoursSnapshot.knownOpen(
                openTime,
                closeTime
        );
    }

    private <T> T requireProjectionValue(
            T value,
            String field
    ) {
        if (value == null) {
            throw new IllegalStateException(
                    "Repository projection returned null for "
                            + field
            );
        }

        return value;
    }
}