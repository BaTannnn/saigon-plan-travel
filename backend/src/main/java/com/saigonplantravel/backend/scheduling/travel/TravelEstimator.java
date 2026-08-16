package com.saigonplantravel.backend.scheduling.travel;

import com.saigonplantravel.backend.scheduling.model.TravelEstimate;
import java.math.BigDecimal;

public interface TravelEstimator {

    TravelEstimate estimate(
            BigDecimal fromLatitude, BigDecimal fromLongitude, BigDecimal toLatitude, BigDecimal toLongitude);
}
