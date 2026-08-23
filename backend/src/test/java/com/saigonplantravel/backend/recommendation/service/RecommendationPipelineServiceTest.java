package com.saigonplantravel.backend.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.recommendation.filter.BudgetCandidateFilter;
import com.saigonplantravel.backend.recommendation.filter.OpeningHoursCandidateFilter;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationPipelineServiceTest {

    @Mock
    private PlaceRecommendationService placeRecommendationService;

    @Mock
    private OpeningHoursCandidateFilter openingHoursCandidateFilter;

    @Mock
    private BudgetCandidateFilter budgetCandidateFilter;

    @Mock
    private Trip trip;

    private RecommendationCandidate candidateA;

    private RecommendationCandidate candidateB;

    private RecommendationPipelineService service;

    @BeforeEach
    void setUp() {
        candidateA = new RecommendationCandidate(mock(Place.class), 0.95, "architecture");
        candidateB = new RecommendationCandidate(mock(Place.class), 0.90, "art");
        service = new RecommendationPipelineService(
                placeRecommendationService, openingHoursCandidateFilter, budgetCandidateFilter);
    }

    @Test
    void retrievesAndAppliesCoarseHardFilters() {

        String preference = "Tôi thích kiến trúc và mỹ thuật";

        List<RecommendationCandidate> retrieved = List.of(candidateA, candidateB);

        List<RecommendationCandidate> openingFiltered = List.of(candidateA, candidateB);

        List<RecommendationCandidate> budgetFiltered = List.of(candidateB);

        when(placeRecommendationService.recommend(preference)).thenReturn(retrieved);

        when(openingHoursCandidateFilter.filter(retrieved, trip)).thenReturn(openingFiltered);

        when(budgetCandidateFilter.filter(openingFiltered, trip)).thenReturn(budgetFiltered);

        List<RecommendationCandidate> result = service.recommend(trip, preference);

        assertThat(result).containsExactly(candidateB);

        verify(placeRecommendationService).recommend(preference);

        verify(openingHoursCandidateFilter).filter(retrieved, trip);

        verify(budgetCandidateFilter).filter(openingFiltered, trip);
    }

    @Test
    void returnsEmptyWhenNoCandidateSurvivesFilters() {

        String preference = "Tôi thích kiến trúc";

        List<RecommendationCandidate> retrieved = List.of(candidateA);

        when(placeRecommendationService.recommend(preference)).thenReturn(retrieved);

        when(openingHoursCandidateFilter.filter(retrieved, trip)).thenReturn(List.of());

        when(budgetCandidateFilter.filter(List.of(), trip)).thenReturn(List.of());

        List<RecommendationCandidate> result = service.recommend(trip, preference);

        assertThat(result).isEmpty();
    }
}
