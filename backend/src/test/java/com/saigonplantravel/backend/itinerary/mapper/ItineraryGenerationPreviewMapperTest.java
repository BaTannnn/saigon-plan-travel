package com.saigonplantravel.backend.itinerary.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.itinerary.dto.ItineraryGenerationPreviewResponse;
import com.saigonplantravel.backend.itinerary.model.GeneratedItineraryPreview;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItineraryGenerationPreviewMapperTest {

    private final ItineraryGenerationPreviewMapper mapper = new ItineraryGenerationPreviewMapper();

    @Test
    void mapsReasonsByPlaceSlugAndKeepsMissingReasonNull() {
        Place firstPlace = place("place-a", "Place A");
        Place secondPlace = place("place-b", "Place B");
        ItineraryPlan plan = new ItineraryPlan(
                List.of(stop(firstPlace, LocalTime.of(8, 0)), stop(secondPlace, LocalTime.of(9, 0))),
                BigDecimal.ZERO,
                0,
                120,
                0.0);
        GeneratedItineraryPreview preview =
                new GeneratedItineraryPreview(plan, Map.of("place-b", "Lý do cho địa điểm B"));

        ItineraryGenerationPreviewResponse response = mapper.toResponse(preview);

        assertThat(response.stops()).extracting(stop -> stop.place().slug()).containsExactly("place-a", "place-b");
        assertThat(response.stops().get(0).reason()).isNull();
        assertThat(response.stops().get(1).reason()).isEqualTo("Lý do cho địa điểm B");
    }

    private Place place(String slug, String name) {
        return new Place(
                name, slug, "Address", BigDecimal.ONE, BigDecimal.ONE, 60, BigDecimal.ZERO, BigDecimal.ZERO, false);
    }

    private ScheduledStop stop(Place place, LocalTime start) {
        return new ScheduledStop(place, start, start, start.plusHours(1), 0, 0.0, BigDecimal.ZERO);
    }
}
