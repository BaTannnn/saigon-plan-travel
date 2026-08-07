package com.saigonplantravel.backend.scheduling.domain.travel;

import java.math.BigDecimal;

public interface DistanceCalculator {

    BigDecimal calculateKilometers(
            GeoPoint from,
            GeoPoint to
    );
}