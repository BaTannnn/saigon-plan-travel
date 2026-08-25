package com.saigonplantravel.backend.scheduling.travel;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class HaversineDistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    public double calculateKm(
            BigDecimal fromLatitude, BigDecimal fromLongitude, BigDecimal toLatitude, BigDecimal toLongitude) {

        double fromLatRad = Math.toRadians(fromLatitude.doubleValue());

        double toLatRad = Math.toRadians(toLatitude.doubleValue());

        double latitudeDelta = Math.toRadians(toLatitude.subtract(fromLatitude).doubleValue());

        double longitudeDelta =
                Math.toRadians(toLongitude.subtract(fromLongitude).doubleValue());

        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(fromLatRad) * Math.cos(toLatRad) * Math.pow(Math.sin(longitudeDelta / 2), 2);

        double clampedA = Math.clamp(a, 0.0, 1.0);

        double c = 2 * Math.atan2(Math.sqrt(clampedA), Math.sqrt(1.0 - clampedA));

        return EARTH_RADIUS_KM * c;
    }
}
