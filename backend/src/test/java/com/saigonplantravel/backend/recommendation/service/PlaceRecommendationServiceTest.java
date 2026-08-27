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
import java.math.BigDecimal;
import java.util.List;
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
    private com.saigonplantravel.backend.trip.entity.Trip trip;

    private PlaceRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new PlaceRecommendationService(
                semanticPlaceRetriever, placeQueryService, coarsePlaceEligibilityService);
    }

    @Test
    void trimsPreferenceAndRequestsFifteenSemanticCandidates() {
        Place eligiblePlace = place("Place A", "place-a");
        when(placeQueryService.findAllActiveForScheduling()).thenReturn(List.of(eligiblePlace));
        when(coarsePlaceEligibilityService.findEligiblePlaces(List.of(eligiblePlace), trip))
                .thenReturn(List.of(eligiblePlace));
        when(semanticPlaceRetriever.retrieve("Tôi thích kiến trúc và mỹ thuật", 15, List.of("place-a")))
                .thenReturn(List.of());

        service.recommend(trip, "  Tôi thích kiến trúc và mỹ thuật  ");

        verify(semanticPlaceRetriever).retrieve("Tôi thích kiến trúc và mỹ thuật", 15, List.of("place-a"));
    }

    @Test
    void skipsAiRequestWhenNoPlaceIsCoarselyEligible() {
        Place activePlace = place("Place A", "place-a");
        when(placeQueryService.findAllActiveForScheduling()).thenReturn(List.of(activePlace));
        when(coarsePlaceEligibilityService.findEligiblePlaces(List.of(activePlace), trip))
                .thenReturn(List.of());

        List<RecommendationCandidate> result = service.recommend(trip, "kiến trúc");

        assertThat(result).isEmpty();
        verifyNoInteractions(semanticPlaceRetriever);
    }

    @Test
    void returnsEmptyWhenAiReturnsNoCandidates() {
        Place eligiblePlace = place("Place A", "place-a");
        stubEligiblePlaces(eligiblePlace);
        when(semanticPlaceRetriever.retrieve("kiến trúc", 15, List.of("place-a"))).thenReturn(List.of());

        assertThat(service.recommend(trip, "kiến trúc")).isEmpty();
    }

    @Test
    void mapsAiCandidatesToCanonicalDatabasePlaces() {
        SemanticPlaceCandidate semanticCandidate = candidate("place-a", "architecture", 0.93);
        Place canonicalPlace = place("Canonical database name", "place-a");
        stubEligiblePlaces(canonicalPlace);
        when(semanticPlaceRetriever.retrieve("architecture", 15, List.of("place-a")))
                .thenReturn(List.of(semanticCandidate));

        List<RecommendationCandidate> result = service.recommend(trip, "architecture");

        assertThat(result).singleElement().satisfies(recommendation -> {
            assertThat(recommendation.place()).isSameAs(canonicalPlace);
            assertThat(recommendation.place().getName()).isEqualTo("Canonical database name");
            assertThat(recommendation.semanticScore()).isEqualTo(0.93);
            assertThat(recommendation.matchedSection()).isEqualTo("architecture");
        });
    }

    @Test
    void dropsAiCandidatesNotReturnedByActivePlaceRepository() {
        List<SemanticPlaceCandidate> semanticCandidates = List.of(
                candidate("place-a", "section-a", 0.95),
                candidate("place-b", "section-b", 0.90),
                candidate("place-c", "section-c", 0.85));
        Place placeA = place("Database A", "place-a");
        Place placeC = place("Database C", "place-c");
        stubEligiblePlaces(placeA, placeC);
        when(semanticPlaceRetriever.retrieve("culture", 15, List.of("place-a", "place-c")))
                .thenReturn(semanticCandidates);

        List<RecommendationCandidate> result = service.recommend(trip, "culture");

        assertThat(result).extracting(candidate -> candidate.place().getSlug()).containsExactly("place-a", "place-c");
    }

    @Test
    void preservesAiCandidateOrderRegardlessOfRepositoryOrder() {
        List<SemanticPlaceCandidate> semanticCandidates = List.of(
                candidate("place-c", "section-c", 0.97),
                candidate("place-a", "section-a", 0.92),
                candidate("place-b", "section-b", 0.88));
        Place placeA = place("Database A", "place-a");
        Place placeB = place("Database B", "place-b");
        Place placeC = place("Database C", "place-c");
        stubEligiblePlaces(placeA, placeB, placeC);
        when(semanticPlaceRetriever.retrieve("art", 15, List.of("place-a", "place-b", "place-c")))
                .thenReturn(semanticCandidates);

        List<RecommendationCandidate> result = service.recommend(trip, "art");

        assertThat(result)
                .extracting(candidate -> candidate.place().getSlug())
                .containsExactly("place-c", "place-a", "place-b");
    }

    private void stubEligiblePlaces(Place... places) {
        List<Place> activePlaces = List.of(places);
        when(placeQueryService.findAllActiveForScheduling()).thenReturn(activePlaces);
        when(coarsePlaceEligibilityService.findEligiblePlaces(activePlaces, trip))
                .thenReturn(activePlaces);
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
