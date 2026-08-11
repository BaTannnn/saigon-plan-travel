package com.saigonplantravel.backend.itinerary.mapper;

import com.saigonplantravel.backend.itinerary.dto.ItineraryItemResponse;
import com.saigonplantravel.backend.itinerary.dto.ItineraryResponse;
import com.saigonplantravel.backend.itinerary.entity.Itinerary;
import com.saigonplantravel.backend.place.mapper.PlaceMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ItineraryMapper {

    private final PlaceMapper placeMapper;

    public ItineraryMapper(PlaceMapper placeMapper) {
        this.placeMapper = placeMapper;
    }

    public ItineraryResponse toResponse(UUID tripPublicId, Itinerary itinerary) {
        List<ItineraryItemResponse> items = itinerary.getItems().stream()
                .map(item -> new ItineraryItemResponse(
                        item.getPublicId(),
                        item.getSequenceNo(),
                        placeMapper.toSummaryResponse(item.getPlace()),
                        item.getCreatedAt(),
                        item.getUpdatedAt()))
                .toList();

        return new ItineraryResponse(
                itinerary.getPublicId(), tripPublicId, items, itinerary.getCreatedAt(), itinerary.getUpdatedAt());
    }

    public ItineraryResponse toEmptyResponse(UUID tripPublicId) {
        return new ItineraryResponse(null, tripPublicId, List.of(), null, null);
    }
}
