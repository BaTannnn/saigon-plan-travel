package com.saigonplantravel.backend.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.service.PlaceQueryService;
import com.saigonplantravel.backend.recommendation.SemanticPlaceRetriever;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
import com.saigonplantravel.backend.recommendation.model.SemanticPlaceCandidate;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceRecommendationServiceTest {

    @Mock
    private SemanticPlaceRetriever semanticPlaceRetriever;

    @Mock
    private PlaceQueryService placeQueryService;

    @Mock
    private CoarsePlaceEligibilityService coarsePlaceEligibilityService;

    @Mock
    private Trip trip;

    private PlaceRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new PlaceRecommendationService(
                semanticPlaceRetriever, placeQueryService, coarsePlaceEligibilityService);
    }

    @Test
    void trimsPreferenceAndRequestsThirtySemanticCandidatesWithoutLoadingPlacesFirst() {
        when(semanticPlaceRetriever.retrieve("Tôi thích kiến trúc và mỹ thuật", 30))
                .thenReturn(List.of());

        List<RecommendationCandidate> result = service.recommend(trip, "  Tôi thích kiến trúc và mỹ thuật  ");

        assertThat(result).isEmpty();
        verify(semanticPlaceRetriever).retrieve("Tôi thích kiến trúc và mỹ thuật", 30);
        verifyNoInteractions(placeQueryService, coarsePlaceEligibilityService);
    }

    @Test
    void loadsOnlyTheThirtySemanticCandidateSlugs() {
        List<SemanticPlaceCandidate> semanticCandidates = IntStream.rangeClosed(1, 30)
                .mapToObj(index -> candidate("place-" + index, "section-" + index, 1.0 - index / 100.0))
                .toList();
        List<String> candidateSlugs = semanticCandidates.stream()
                .map(SemanticPlaceCandidate::placeSlug)
                .toList();
        when(semanticPlaceRetriever.retrieve("culture", 30)).thenReturn(semanticCandidates);
        when(placeQueryService.findAllActiveBySlugsForScheduling(candidateSlugs))
                .thenReturn(List.of());
        when(coarsePlaceEligibilityService.findEligiblePlaces(List.of(), trip)).thenReturn(List.of());

        assertThat(service.recommend(trip, "culture")).isEmpty();

        verify(placeQueryService).findAllActiveBySlugsForScheduling(candidateSlugs);
        verify(coarsePlaceEligibilityService).findEligiblePlaces(List.of(), trip);
    }

    @Test
    void appliesCoarseEligibilityAfterSemanticRetrieval() {
        List<SemanticPlaceCandidate> semanticCandidates =
                List.of(candidate("place-a", "section-a", 0.95), candidate("place-b", "section-b", 0.90));
        Place placeA = place("Database A", "place-a");
        Place placeB = place("Database B", "place-b");
        List<Place> loadedPlaces = List.of(placeA, placeB);
        when(semanticPlaceRetriever.retrieve("culture", 30)).thenReturn(semanticCandidates);
        when(placeQueryService.findAllActiveBySlugsForScheduling(List.of("place-a", "place-b")))
                .thenReturn(loadedPlaces);
        when(coarsePlaceEligibilityService.findEligiblePlaces(loadedPlaces, trip))
                .thenReturn(List.of(placeA));

        List<RecommendationCandidate> result = service.recommend(trip, "culture");

        assertThat(result).singleElement().satisfies(recommendation -> {
            assertThat(recommendation.place()).isSameAs(placeA);
            assertThat(recommendation.semanticScore()).isEqualTo(0.95);
        });
    }

    @Test
    void preservesSemanticOrderScoresAndMetadataWhenRepositoryOrderDiffers() {
        List<SemanticPlaceCandidate> semanticCandidates = List.of(
                candidate("place-a", "section-a", 0.91),
                candidate("place-b", "section-b", 0.82),
                candidate("place-c", "section-c", 0.73));
        Place placeA = place("Database A", "place-a");
        Place placeB = place("Database B", "place-b");
        Place placeC = place("Database C", "place-c");
        List<Place> repositoryOrder = List.of(placeC, placeA, placeB);
        when(semanticPlaceRetriever.retrieve("art", 30)).thenReturn(semanticCandidates);
        when(placeQueryService.findAllActiveBySlugsForScheduling(List.of("place-a", "place-b", "place-c")))
                .thenReturn(repositoryOrder);
        when(coarsePlaceEligibilityService.findEligiblePlaces(repositoryOrder, trip))
                .thenReturn(repositoryOrder);

        List<RecommendationCandidate> result = service.recommend(trip, "art");

        assertThat(result)
                .extracting(candidate -> candidate.place().getSlug())
                .containsExactly("place-a", "place-b", "place-c");
        assertThat(result).extracting(RecommendationCandidate::semanticScore).containsExactly(0.91, 0.82, 0.73);
        assertThat(result)
                .extracting(RecommendationCandidate::matchedSection)
                .containsExactly("section-a", "section-b", "section-c");
    }

    @Test
    void dropsSemanticCandidatesMissingFromActivePlaceQuery() {
        List<SemanticPlaceCandidate> semanticCandidates =
                List.of(candidate("place-a", "section-a", 0.95), candidate("place-b", "section-b", 0.90));
        Place placeA = place("Database A", "place-a");
        when(semanticPlaceRetriever.retrieve("culture", 30)).thenReturn(semanticCandidates);
        when(placeQueryService.findAllActiveBySlugsForScheduling(List.of("place-a", "place-b")))
                .thenReturn(List.of(placeA));
        when(coarsePlaceEligibilityService.findEligiblePlaces(List.of(placeA), trip))
                .thenReturn(List.of(placeA));

        List<RecommendationCandidate> result = service.recommend(trip, "culture");

        assertThat(result).extracting(candidate -> candidate.place().getSlug()).containsExactly("place-a");
    }

    private static SemanticPlaceCandidate candidate(String slug, String matchedSection, double semanticScore) {
        return new SemanticPlaceCandidate(slug, semanticScore, matchedSection);
    }

    private static Place place(String name, String slug) {
        return new Place(
                name,
                slug,
                "Ho Chi Minh City",
                new BigDecimal("10.7769"),
                new BigDecimal("106.7009"),
                60,
                BigDecimal.ZERO,
                new BigDecimal("100000"),
                true);
    }
}
