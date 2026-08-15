package com.saigonplantravel.backend.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.recommendation.filter.BudgetCandidateFilter;
import com.saigonplantravel.backend.recommendation.filter.OpeningHoursCandidateFilter;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.model.ScoredCandidate;
import com.saigonplantravel.backend.recommendation.ranking.CandidateRanker;
import com.saigonplantravel.backend.recommendation.scoring.CandidateScorer;
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
    private CandidateScorer candidateScorer;

    @Mock
    private CandidateRanker candidateRanker;

    @Mock
    private Trip trip;

    @Mock
    private RecommendationCandidate candidateA;

    @Mock
    private RecommendationCandidate candidateB;

    @Mock
    private ScoredCandidate scoredA;

    @Mock
    private ScoredCandidate scoredB;

    private RecommendationPipelineService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationPipelineService(
                placeRecommendationService,
                openingHoursCandidateFilter,
                budgetCandidateFilter,
                candidateScorer,
                candidateRanker);
    }

    @Test
    void retrievesFiltersScoresAndRanksCandidates() {

        String preference = "Tôi thích kiến trúc và mỹ thuật";

        List<RecommendationCandidate> retrieved = List.of(candidateA, candidateB);

        List<RecommendationCandidate> openingFiltered = List.of(candidateA, candidateB);

        List<RecommendationCandidate> budgetFiltered = List.of(candidateA, candidateB);

        List<ScoredCandidate> scored = List.of(scoredA, scoredB);

        List<ScoredCandidate> ranked = List.of(scoredB, scoredA);

        when(placeRecommendationService.recommend(preference)).thenReturn(retrieved);

        when(openingHoursCandidateFilter.filter(retrieved, trip)).thenReturn(openingFiltered);

        when(budgetCandidateFilter.filter(openingFiltered, trip)).thenReturn(budgetFiltered);

        when(candidateScorer.score(candidateA, trip)).thenReturn(scoredA);

        when(candidateScorer.score(candidateB, trip)).thenReturn(scoredB);

        when(candidateRanker.rank(scored)).thenReturn(ranked);

        List<ScoredCandidate> result = service.recommend(trip, preference);

        assertThat(result).containsExactly(scoredB, scoredA);

        verify(placeRecommendationService).recommend(preference);

        verify(openingHoursCandidateFilter).filter(retrieved, trip);

        verify(budgetCandidateFilter).filter(openingFiltered, trip);

        verify(candidateRanker).rank(scored);
    }

    @Test
    void returnsEmptyWhenNoCandidateSurvivesHardFilters() {

        String preference = "Tôi thích kiến trúc";

        List<RecommendationCandidate> retrieved = List.of(candidateA);

        when(placeRecommendationService.recommend(preference)).thenReturn(retrieved);

        when(openingHoursCandidateFilter.filter(retrieved, trip)).thenReturn(List.of());

        when(budgetCandidateFilter.filter(List.of(), trip)).thenReturn(List.of());

        when(candidateRanker.rank(List.of())).thenReturn(List.of());

        List<ScoredCandidate> result = service.recommend(trip, preference);

        assertThat(result).isEmpty();
    }
}
