package com.saigonplantravel.backend.itinerary.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.itinerary.dto.ItineraryGenerationPreviewResponse;
import com.saigonplantravel.backend.itinerary.model.GeneratedItineraryPreview;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItineraryGenerationPreviewMapperTest {

    private final ItineraryGenerationPreviewMapper mapper = new ItineraryGenerationPreviewMapper();

    @Test
    void mapsReasonsByPlaceSlugAndKeepsMissingReasonNull() {
        SchedulingPlace firstPlace = place("place-a", "Place A");
        SchedulingPlace secondPlace = place("place-b", "Place B");
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

    private SchedulingPlace place(String slug, String name) {
        return new SchedulingPlace(
                slug,
                name,
                BigDecimal.ONE,
                BigDecimal.ONE,
                60,
                BigDecimal.ZERO,
                false,
                null,
                OpeningWindow.open(LocalTime.of(8, 0), LocalTime.of(18, 0)));
    }

    private ScheduledStop stop(SchedulingPlace place, LocalTime start) {
        return new ScheduledStop(
                place, start, start, start.plusHours(1), 0, 0.0, BigDecimal.ZERO, true, true);
    }
}
