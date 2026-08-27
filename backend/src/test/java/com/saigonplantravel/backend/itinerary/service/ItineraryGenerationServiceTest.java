package com.saigonplantravel.backend.itinerary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.ai.client.AiItineraryExplanationClient;
import com.saigonplantravel.backend.itinerary.mapper.SchedulingInputMapper;
import com.saigonplantravel.backend.itinerary.model.GeneratedItineraryPreview;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.service.RecommendationPipelineService;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.PlanningContext;
import com.saigonplantravel.backend.scheduling.model.ScheduledStop;
import com.saigonplantravel.backend.scheduling.model.SchedulingCandidate;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import com.saigonplantravel.backend.scheduling.service.ItineraryScheduler;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import com.saigonplantravel.backend.trip.service.TripQueryService;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItineraryGenerationServiceTest {

    @Mock
    private TripQueryService tripQueryService;

    @Mock
    private RecommendationPipelineService recommendationPipelineService;

    @Mock
    private SchedulingInputMapper schedulingInputMapper;

    @Mock
    private ItineraryScheduler itineraryScheduler;

    @Mock
    private AiItineraryExplanationClient itineraryExplanationClient;

    @Mock
    private Trip trip;

    private RecommendationCandidate recommendationCandidate;
    private SchedulingCandidate schedulingCandidate;
    private PlanningContext planningContext;
    private ItineraryGenerationService service;

    @BeforeEach
    void setUp() {
        recommendationCandidate = new RecommendationCandidate(mock(Place.class), 0.95, "architecture");
        schedulingCandidate = new SchedulingCandidate(place("candidate", "Candidate"), 0.95, "architecture");
        planningContext = new PlanningContext(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000"),
                BigDecimal.ONE,
                BigDecimal.ONE,
                EnvironmentPreference.MIXED);
        service = new ItineraryGenerationService(
                tripQueryService,
                recommendationPipelineService,
                schedulingInputMapper,
                itineraryScheduler,
                itineraryExplanationClient);
    }

    @Test
    void generatesPlanFromRecommendedCandidates() {
        Long userId = 1L;
        UUID tripPublicId = UUID.randomUUID();
        String preference = "Tôi thích kiến trúc cổ và mỹ thuật";
        List<RecommendationCandidate> recommendations = List.of(recommendationCandidate);
        List<SchedulingCandidate> schedulingCandidates = List.of(schedulingCandidate);
        ItineraryPlan expectedPlan = new ItineraryPlan(List.of(), BigDecimal.ZERO, 0, 0, 0.0);

        when(tripQueryService.findOwnedTrip(userId, tripPublicId)).thenReturn(trip);
        when(recommendationPipelineService.recommend(trip, preference)).thenReturn(recommendations);
        when(schedulingInputMapper.toPlanningContext(trip)).thenReturn(planningContext);
        when(schedulingInputMapper.toSchedulingCandidates(trip, recommendations)).thenReturn(schedulingCandidates);
        when(itineraryScheduler.schedule(planningContext, schedulingCandidates)).thenReturn(expectedPlan);

        ItineraryPlan result = service.generatePlan(userId, tripPublicId, preference);

        assertThat(result).isSameAs(expectedPlan);
        verify(recommendationPipelineService).recommend(trip, preference);
        verify(itineraryScheduler).schedule(planningContext, schedulingCandidates);
    }

    @Test
    void throwsWhenTripDoesNotBelongToUser() {
        Long userId = 1L;
        UUID tripPublicId = UUID.randomUUID();

        when(tripQueryService.findOwnedTrip(userId, tripPublicId)).thenThrow(new TripNotFoundException());

        assertThatThrownBy(() -> service.generatePlan(userId, tripPublicId, "Tôi thích bảo tàng"))
                .isInstanceOf(TripNotFoundException.class);
    }

    @Test
    void enrichesScheduledPlacesWithoutChangingSchedulerOutput() {
        Long userId = 1L;
        UUID tripPublicId = UUID.randomUUID();
        String preference = "Tôi thích thiên nhiên và nghệ thuật";
        ItineraryPlan plan = planWith(place("place-a", "Place A"), place("place-b", "Place B"));

        stubGeneratedPlan(userId, tripPublicId, preference, plan);
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

        stubGeneratedPlan(userId, tripPublicId, preference, plan);
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

        stubGeneratedPlan(userId, tripPublicId, preference, plan);

        GeneratedItineraryPreview result = service.generatePreview(userId, tripPublicId, preference);

        assertThat(result.plan()).isSameAs(plan);
        assertThat(result.reasonsByPlaceSlug()).isEmpty();
        verify(itineraryExplanationClient, never()).generateReasons(preference, List.of());
    }

    private void stubGeneratedPlan(
            Long userId, UUID tripPublicId, String preference, ItineraryPlan plan) {
        List<RecommendationCandidate> recommendations = List.of(recommendationCandidate);
        List<SchedulingCandidate> schedulingCandidates = List.of(schedulingCandidate);

        when(tripQueryService.findOwnedTrip(userId, tripPublicId)).thenReturn(trip);
        when(recommendationPipelineService.recommend(trip, preference)).thenReturn(recommendations);
        when(schedulingInputMapper.toPlanningContext(trip)).thenReturn(planningContext);
        when(schedulingInputMapper.toSchedulingCandidates(trip, recommendations)).thenReturn(schedulingCandidates);
        when(itineraryScheduler.schedule(planningContext, schedulingCandidates)).thenReturn(plan);
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

    private ItineraryPlan planWith(SchedulingPlace... places) {
        List<ScheduledStop> stops = Arrays.stream(places)
                .map(place -> new ScheduledStop(
                        place,
                        LocalTime.of(8, 0),
                        LocalTime.of(8, 0),
                        LocalTime.of(9, 0),
                        0,
                        0.0,
                        BigDecimal.ZERO,
                        true,
                        true))
                .toList();
        return new ItineraryPlan(stops, BigDecimal.ZERO, 0, stops.size() * 60, 0.0);
    }
}
