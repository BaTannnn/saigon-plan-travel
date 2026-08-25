package com.saigonplantravel.backend.itinerary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.ai.client.AiItineraryExplanationClient;
import com.saigonplantravel.backend.itinerary.model.GeneratedItineraryPreview;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.service.RecommendationPipelineService;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.service.ItineraryScheduler;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import com.saigonplantravel.backend.trip.repository.TripRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItineraryGenerationServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private RecommendationPipelineService recommendationPipelineService;

    @Mock
    private ItineraryScheduler itineraryScheduler;

    @Mock
    private AiItineraryExplanationClient itineraryExplanationClient;

    @Mock
    private Trip trip;

    private RecommendationCandidate candidateA;

    private ItineraryGenerationService service;

    @BeforeEach
    void setUp() {
        candidateA = new RecommendationCandidate(mock(Place.class), 0.95, "architecture");
        service = new ItineraryGenerationService(
                tripRepository, recommendationPipelineService, itineraryScheduler, itineraryExplanationClient);
    }

    @Test
    void generatesPlanFromRecommendedCandidates() {

        Long userId = 1L;
        UUID tripPublicId = UUID.randomUUID();

        String preference = "Tôi thích kiến trúc cổ và mỹ thuật";

        List<RecommendationCandidate> candidates = List.of(candidateA);

        ItineraryPlan expectedPlan = new ItineraryPlan(List.of(), BigDecimal.ZERO, 0, 0, 0.0);

        when(tripRepository.findByPublicIdAndUserId(tripPublicId, userId)).thenReturn(Optional.of(trip));

        when(recommendationPipelineService.recommend(trip, preference)).thenReturn(candidates);

        when(itineraryScheduler.schedule(trip, candidates)).thenReturn(expectedPlan);

        ItineraryPlan result = service.generatePlan(userId, tripPublicId, preference);

        assertThat(result).isSameAs(expectedPlan);

        verify(recommendationPipelineService).recommend(trip, preference);

        verify(itineraryScheduler).schedule(trip, candidates);
    }

    @Test
    void throwsWhenTripDoesNotBelongToUser() {

        Long userId = 1L;
        UUID tripPublicId = UUID.randomUUID();

        when(tripRepository.findByPublicIdAndUserId(tripPublicId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generatePlan(userId, tripPublicId, "Tôi thích bảo tàng"))
                .isInstanceOf(TripNotFoundException.class);
    }

    @Test
    void enrichesScheduledPlacesWithoutChangingSchedulerOutput() {
        Long userId = 1L;
        UUID tripPublicId = UUID.randomUUID();
        String preference = "Tôi thích thiên nhiên và nghệ thuật";
        Place firstPlace = place("place-a", "Place A");
        Place secondPlace = place("place-b", "Place B");
        List<RecommendationCandidate> candidates = List.of(candidateA);
        ItineraryPlan plan = planWith(firstPlace, secondPlace);

        when(tripRepository.findByPublicIdAndUserId(tripPublicId, userId)).thenReturn(Optional.of(trip));
        when(recommendationPipelineService.recommend(trip, preference)).thenReturn(candidates);
        when(itineraryScheduler.schedule(trip, candidates)).thenReturn(plan);
        when(itineraryExplanationClient.generateReasons(preference, List.of("place-a", "place-b")))
                .thenReturn(Map.of("place-a", "Lý do A", "place-b", "Lý do B"));

        GeneratedItineraryPreview result = service.generatePreview(userId, tripPublicId, preference);

        assertThat(result.plan()).isSameAs(plan);
        assertThat(result.plan().stops()).containsExactlyElementsOf(plan.stops());
        assertThat(result.reasonsByPlaceSlug())
                .containsExactlyInAnyOrderEntriesOf(Map.of("place-a", "Lý do A", "place-b", "Lý do B"));
        verify(itineraryExplanationClient).generateReasons(preference, List.of("place-a", "place-b"));
    }

    @Test
    void returnsScheduledPreviewWhenExplanationFails() {
        Long userId = 1L;
        UUID tripPublicId = UUID.randomUUID();
        String preference = "Tôi thích thiên nhiên";
        ItineraryPlan plan = planWith(place("place-a", "Place A"));

        when(tripRepository.findByPublicIdAndUserId(tripPublicId, userId)).thenReturn(Optional.of(trip));
        when(recommendationPipelineService.recommend(trip, preference)).thenReturn(List.of(candidateA));
        when(itineraryScheduler.schedule(trip, List.of(candidateA))).thenReturn(plan);
        when(itineraryExplanationClient.generateReasons(preference, List.of("place-a")))
                .thenThrow(new RuntimeException("AI unavailable"));

        GeneratedItineraryPreview result = service.generatePreview(userId, tripPublicId, preference);

        assertThat(result.plan()).isSameAs(plan);
        assertThat(result.reasonsByPlaceSlug()).isEmpty();
    }

    @Test
    void skipsExplanationRequestForEmptySchedule() {
        Long userId = 1L;
        UUID tripPublicId = UUID.randomUUID();
        String preference = "Tôi thích thiên nhiên";
        ItineraryPlan plan = new ItineraryPlan(List.of(), BigDecimal.ZERO, 0, 0, 0.0);

        when(tripRepository.findByPublicIdAndUserId(tripPublicId, userId)).thenReturn(Optional.of(trip));
        when(recommendationPipelineService.recommend(trip, preference)).thenReturn(List.of(candidateA));
        when(itineraryScheduler.schedule(trip, List.of(candidateA))).thenReturn(plan);

        GeneratedItineraryPreview result = service.generatePreview(userId, tripPublicId, preference);

        assertThat(result.plan()).isSameAs(plan);
        assertThat(result.reasonsByPlaceSlug()).isEmpty();
        verify(itineraryExplanationClient, never()).generateReasons(preference, List.of());
    }

    private Place place(String slug, String name) {
        return new Place(
                name, slug, "Address", BigDecimal.ONE, BigDecimal.ONE, 60, BigDecimal.ZERO, BigDecimal.ZERO, false);
    }

    private ItineraryPlan planWith(Place... places) {
        List<ScheduledStop> stops = java.util.Arrays.stream(places)
                .map(place -> new ScheduledStop(
                        place, LocalTime.of(8, 0), LocalTime.of(8, 0), LocalTime.of(9, 0), 0, 0.0, BigDecimal.ZERO))
                .toList();
        return new ItineraryPlan(stops, BigDecimal.ZERO, 0, stops.size() * 60, 0.0);
    }
}
