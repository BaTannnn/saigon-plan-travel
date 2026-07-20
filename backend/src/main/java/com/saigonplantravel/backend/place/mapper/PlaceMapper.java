package com.saigonplantravel.backend.place.mapper;

import com.saigonplantravel.backend.place.dto.PlaceSummaryResponse;
import com.saigonplantravel.backend.place.entity.Place;
import org.springframework.stereotype.Component;

@Component
public class PlaceMapper {

    public PlaceSummaryResponse toSummaryResponse(Place place) {
        return new PlaceSummaryResponse(
                place.getId(),
                place.getName(),
                place.getSlug(),
                place.getShortDescription(),
                place.getDistrict(),
                place.getLatitude(),
                place.getLongitude(),
                place.getEstimatedVisitMinutes(),
                place.getMinCost(),
                place.getMaxCost(),
                place.getIndoor()
        );
    }
}
