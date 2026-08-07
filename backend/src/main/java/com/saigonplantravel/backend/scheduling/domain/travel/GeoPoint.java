package com.saigonplantravel.backend.scheduling.domain.travel;

import java.math.BigDecimal;
import java.util.Objects;

public record GeoPoint(
        BigDecimal latitude,
        BigDecimal longitude
) {

    private static final BigDecimal MIN_LATITUDE =
            new BigDecimal("-90");

    private static final BigDecimal MAX_LATITUDE =
            new BigDecimal("90");

    private static final BigDecimal MIN_LONGITUDE =
            new BigDecimal("-180");

    private static final BigDecimal MAX_LONGITUDE =
            new BigDecimal("180");

    public GeoPoint {
        Objects.requireNonNull(
                latitude,
                "latitude must not be null"
        );

        Objects.requireNonNull(
                longitude,
                "longitude must not be null"
        );

        if (latitude.compareTo(MIN_LATITUDE) < 0
                || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw new IllegalArgumentException(
                    "latitude must be between -90 and 90"
            );
        }

        if (longitude.compareTo(MIN_LONGITUDE) < 0
                || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new IllegalArgumentException(
                    "longitude must be between -180 and 180"
            );
        }
    }
}