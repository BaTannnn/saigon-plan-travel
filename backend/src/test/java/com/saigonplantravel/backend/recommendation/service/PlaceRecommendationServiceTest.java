package com.saigonplantravel.backend.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.ai.client.AiRecommendationClient;
import com.saigonplantravel.backend.ai.client.dto.AiPlaceCandidateResponse;
import com.saigonplantravel.backend.ai.client.dto.AiPlaceRecommendationResponse;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.repository.PlaceRepository;
import com.saigonplantravel.backend.recommendation.model.RecommendationCandidate;
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
    private AiRecommendationClient aiRecommendationClient;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private CoarsePlaceEligibilityService coarsePlaceEligibilityService;

    @Mock
    private com.saigonplantravel.backend.trip.entity.Trip trip;

    private PlaceRecommendationService service;

    @BeforeEach
    void setUp() {
        service =
                new PlaceRecommendationService(aiRecommendationClient, placeRepository, coarsePlaceEligibilityService);
    }

    @Test
    void trimsPreferenceAndRequestsFifteenSemanticCandidates() {
        Place eligiblePlace = place("Place A", "place-a");
        when(placeRepository.findAllActiveForScheduling()).thenReturn(List.of(eligiblePlace));
        when(coarsePlaceEligibilityService.findEligiblePlaces(List.of(eligiblePlace), trip))
                .thenReturn(List.of(eligiblePlace));
        when(aiRecommendationClient.recommendPlaces("Tôi thích kiến trúc và mỹ thuật", 15, List.of("place-a")))
                .thenReturn(new AiPlaceRecommendationResponse(List.of()));

        service.recommend(trip, "  Tôi thích kiến trúc và mỹ thuật  ");

        verify(aiRecommendationClient).recommendPlaces("Tôi thích kiến trúc và mỹ thuật", 15, List.of("place-a"));
    }

    @Test
    void skipsAiRequestWhenNoPlaceIsCoarselyEligible() {
        Place activePlace = place("Place A", "place-a");
        when(placeRepository.findAllActiveForScheduling()).thenReturn(List.of(activePlace));
        when(coarsePlaceEligibilityService.findEligiblePlaces(List.of(activePlace), trip))
                .thenReturn(List.of());

        List<RecommendationCandidate> result = service.recommend(trip, "kiến trúc");

        assertThat(result).isEmpty();
        verifyNoInteractions(aiRecommendationClient);
    }

    @Test
    void returnsEmptyWhenAiReturnsNoCandidates() {
        Place eligiblePlace = place("Place A", "place-a");
        stubEligiblePlaces(eligiblePlace);
        when(aiRecommendationClient.recommendPlaces("kiến trúc", 15, List.of("place-a")))
                .thenReturn(new AiPlaceRecommendationResponse(List.of()));

        assertThat(service.recommend(trip, "kiến trúc")).isEmpty();
    }

    @Test
    void mapsAiCandidatesToCanonicalDatabasePlaces() {
        AiPlaceCandidateResponse aiCandidate = candidate("place-a", "AI place name", "architecture", 0.93);
        Place canonicalPlace = place("Canonical database name", "place-a");
        stubEligiblePlaces(canonicalPlace);
        when(aiRecommendationClient.recommendPlaces("architecture", 15, List.of("place-a")))
                .thenReturn(new AiPlaceRecommendationResponse(List.of(aiCandidate)));

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
        List<AiPlaceCandidateResponse> aiCandidates = List.of(
                candidate("place-a", "AI A", "section-a", 0.95),
                candidate("place-b", "AI B", "section-b", 0.90),
                candidate("place-c", "AI C", "section-c", 0.85));
        Place placeA = place("Database A", "place-a");
        Place placeC = place("Database C", "place-c");
        stubEligiblePlaces(placeA, placeC);
        when(aiRecommendationClient.recommendPlaces("culture", 15, List.of("place-a", "place-c")))
                .thenReturn(new AiPlaceRecommendationResponse(aiCandidates));

        List<RecommendationCandidate> result = service.recommend(trip, "culture");

        assertThat(result).extracting(candidate -> candidate.place().getSlug()).containsExactly("place-a", "place-c");
    }

    @Test
    void preservesAiCandidateOrderRegardlessOfRepositoryOrder() {
        List<AiPlaceCandidateResponse> aiCandidates = List.of(
                candidate("place-c", "AI C", "section-c", 0.97),
                candidate("place-a", "AI A", "section-a", 0.92),
                candidate("place-b", "AI B", "section-b", 0.88));
        Place placeA = place("Database A", "place-a");
        Place placeB = place("Database B", "place-b");
        Place placeC = place("Database C", "place-c");
        stubEligiblePlaces(placeA, placeB, placeC);
        when(aiRecommendationClient.recommendPlaces("art", 15, List.of("place-a", "place-b", "place-c")))
                .thenReturn(new AiPlaceRecommendationResponse(aiCandidates));

        List<RecommendationCandidate> result = service.recommend(trip, "art");

        assertThat(result)
                .extracting(candidate -> candidate.place().getSlug())
                .containsExactly("place-c", "place-a", "place-b");
    }

    private void stubEligiblePlaces(Place... places) {
        List<Place> activePlaces = List.of(places);
        when(placeRepository.findAllActiveForScheduling()).thenReturn(activePlaces);
        when(coarsePlaceEligibilityService.findEligiblePlaces(activePlaces, trip))
                .thenReturn(activePlaces);
    }

    private static AiPlaceCandidateResponse candidate(
            String slug, String placeName, String matchedSection, double semanticScore) {
        return new AiPlaceCandidateResponse(slug, placeName, matchedSection, semanticScore);
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
