package com.saigonplantravel.backend.place.scheduling;

import com.saigonplantravel.backend.place.domain.AdministrativeUnitType;
import com.saigonplantravel.backend.place.scheduling.OpeningHoursSnapshot;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record PlaceSchedulingCandidate(
        Long placeId,
        String name,
        String slug,
        String address,
        String administrativeUnitName,
        AdministrativeUnitType administrativeUnitType,
        BigDecimal latitude,
        BigDecimal longitude,
        int baseVisitMinutes,
        BigDecimal minCost,
        boolean indoor,
        Set<Long> matchedPreferredCategoryIds,
        OpeningHoursSnapshot openingHours
) {

    private static final BigDecimal MIN_LATITUDE =
            new BigDecimal("-90");

    private static final BigDecimal MAX_LATITUDE =
            new BigDecimal("90");

    private static final BigDecimal MIN_LONGITUDE =
            new BigDecimal("-180");

    private static final BigDecimal MAX_LONGITUDE =
            new BigDecimal("180");

    public PlaceSchedulingCandidate {
        if (placeId == null || placeId <= 0) {
            throw new IllegalArgumentException(
                    "placeId must be positive"
            );
        }

        name = requireText(name, "name");
        slug = requireText(slug, "slug");
        address = requireText(address, "address");

        administrativeUnitName = requireText(
                administrativeUnitName,
                "administrativeUnitName"
        );

        Objects.requireNonNull(
                administrativeUnitType,
                "administrativeUnitType must not be null"
        );

        validateCoordinate(
                latitude,
                MIN_LATITUDE,
                MAX_LATITUDE,
                "latitude"
        );

        validateCoordinate(
                longitude,
                MIN_LONGITUDE,
                MAX_LONGITUDE,
                "longitude"
        );

        if (baseVisitMinutes <= 0) {
            throw new IllegalArgumentException(
                    "baseVisitMinutes must be positive"
            );
        }

        Objects.requireNonNull(
                minCost,
                "minCost must not be null"
        );

        if (minCost.signum() < 0) {
            throw new IllegalArgumentException(
                    "minCost must not be negative"
            );
        }

        matchedPreferredCategoryIds =
                immutableSortedCategoryIds(
                        matchedPreferredCategoryIds
                );

        Objects.requireNonNull(
                openingHours,
                "openingHours must not be null"
        );
    }

    private static String requireText(
            String value,
            String field
    ) {
        Objects.requireNonNull(
                value,
                field + " must not be null"
        );

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        return normalized;
    }

    private static void validateCoordinate(
            BigDecimal value,
            BigDecimal minimum,
            BigDecimal maximum,
            String field
    ) {
        Objects.requireNonNull(
                value,
                field + " must not be null"
        );

        if (value.compareTo(minimum) < 0
                || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(
                    field + " is outside the valid range"
            );
        }
    }

    private static Set<Long> immutableSortedCategoryIds(
            Set<Long> categoryIds
    ) {
        Objects.requireNonNull(
                categoryIds,
                "matchedPreferredCategoryIds must not be null"
        );

        if (categoryIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "matchedPreferredCategoryIds must not be empty"
            );
        }

        TreeSet<Long> sortedIds =
                new TreeSet<>();

        for (Long categoryId : categoryIds) {
            if (categoryId == null
                    || categoryId <= 0) {
                throw new IllegalArgumentException(
                        "matched category IDs must be positive"
                );
            }

            sortedIds.add(categoryId);
        }

        return Collections.unmodifiableSortedSet(
                sortedIds
        );
    }
}