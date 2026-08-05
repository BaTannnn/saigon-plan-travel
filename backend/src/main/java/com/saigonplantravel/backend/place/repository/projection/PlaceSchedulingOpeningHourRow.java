package com.saigonplantravel.backend.place.repository.projection;

import java.time.LocalTime;

public interface PlaceSchedulingOpeningHourRow {

    Long getPlaceId();

    Short getDayOfWeek();

    LocalTime getOpenTime();

    LocalTime getCloseTime();

    Boolean getClosed();
}