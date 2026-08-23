package com.saigonplantravel.backend.itinerary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.service.RecommendationPipelineService;
import com.saigonplantravel.backend.scheduling.model.ItineraryPlan;
import com.saigonplantravel.backend.scheduling.service.ItineraryScheduler;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import com.saigonplantravel.backend.trip.repository.TripRepository;
import java.math.BigDecimal;
import java.util.List;
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
    private Trip trip;

    private RecommendationCandidate candidateA;

    private ItineraryGenerationService service;

    @BeforeEach
    void setUp() {
        candidateA = new RecommendationCandidate(mock(Place.class), 0.95, "architecture");
        service = new ItineraryGenerationService(tripRepository, recommendationPipelineService, itineraryScheduler);
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
}
