package com.saigonplantravel.backend.scheduling.domain;

import java.math.BigDecimal;

public interface DistanceCalculator {

    BigDecimal calculateKilometers(
            GeoPoint from,
            GeoPoint to
    );
}