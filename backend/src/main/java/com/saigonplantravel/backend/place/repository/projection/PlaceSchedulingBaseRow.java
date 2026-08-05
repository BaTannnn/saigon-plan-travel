package com.saigonplantravel.backend.place.repository.projection;

import com.saigonplantravel.backend.place.domain.AdministrativeUnitType;

import java.math.BigDecimal;

public interface PlaceSchedulingBaseRow {

    Long getPlaceId();

    String getName();

    String getSlug();

    String getAddress();

    String getAdministrativeUnitName();

    AdministrativeUnitType getAdministrativeUnitType();

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    Integer getBaseVisitMinutes();

    BigDecimal getMinCost();

    Boolean getIndoor();
}