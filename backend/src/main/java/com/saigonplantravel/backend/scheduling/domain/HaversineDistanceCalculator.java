package com.saigonplantravel.backend.scheduling.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class HaversineDistanceCalculator
        implements DistanceCalculator {

    private static final double EARTH_RADIUS_KILOMETERS =
            6371.0088;

    @Override
    public BigDecimal calculateKilometers(
            GeoPoint from,
            GeoPoint to
    ) {
        Objects.requireNonNull(
                from,
                "from must not be null"
        );

        Objects.requireNonNull(
                to,
                "to must not be null"
        );

        double haversineValue =
                calculateHaversineValue(
                        from,
                        to
                );

        double distanceKilometers =
                calculateDistanceKilometers(
                        haversineValue
                );

        return BigDecimal
                .valueOf(distanceKilometers);
    }

    private static double calculateDistanceKilometers(
            double haversineValue
    ) {
        /*
         * Sai số double đôi khi có thể khiến giá trị
         * hơi nhỏ hơn 0 hoặc hơi lớn hơn 1.
         */
        double normalizedHaversine =
                Math.clamp(
                        haversineValue,
                        0.0,
                        1.0
                );

        double centralAngle =
                2.0
                        * Math.atan2(
                        Math.sqrt(
                                normalizedHaversine
                        ),
                        Math.sqrt(
                                1.0
                                        - normalizedHaversine
                        )
                );

        return EARTH_RADIUS_KILOMETERS
                * centralAngle;
    }

    private static double calculateHaversineValue(
            GeoPoint from,
            GeoPoint to
    ) {
        double fromLatitudeRadians =
                Math.toRadians(
                        from.latitude()
                                .doubleValue()
                );

        double toLatitudeRadians =
                Math.toRadians(
                        to.latitude()
                                .doubleValue()
                );

        double latitudeDifference =
                Math.toRadians(
                        to.latitude()
                                .subtract(
                                        from.latitude()
                                )
                                .doubleValue()
                );

        double longitudeDifference =
                Math.toRadians(
                        to.longitude()
                                .subtract(
                                        from.longitude()
                                )
                                .doubleValue()
                );

        double latitudeComponent =
                Math.sin(
                        latitudeDifference / 2.0
                );

        double longitudeComponent =
                Math.sin(
                        longitudeDifference / 2.0
                );

        return latitudeComponent
                * latitudeComponent
                + Math.cos(fromLatitudeRadians)
                * Math.cos(toLatitudeRadians)
                * longitudeComponent
                * longitudeComponent;
    }
}