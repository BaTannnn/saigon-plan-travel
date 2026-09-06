package com.saigonplantravel.backend.scheduling.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.scheduling.model.OpeningWindow;
import com.saigonplantravel.backend.scheduling.model.ScoredCandidate;
import com.saigonplantravel.backend.scheduling.model.SchedulingPlace;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandidateRankerTest {

    private final CandidateRanker ranker = new CandidateRanker();

    @Test
    void ranksByFinalScoreDescending() {
        ScoredCandidate low = candidate("low", 0.95, 10, 0.80);
        ScoredCandidate high = candidate("high", 0.70, 30, 0.90);

        List<ScoredCandidate> result = ranker.rank(List.of(low, high));

        assertThat(result).extracting(candidate -> candidate.place().slug()).containsExactly("high", "low");
    }

    @Test
    void usesSemanticScoreWhenFinalScoresTie() {
        ScoredCandidate weakerSemantic = candidate("weaker", 0.70, 10, 0.90);
        ScoredCandidate strongerSemantic = candidate("stronger", 0.90, 30, 0.90);

        List<ScoredCandidate> result = ranker.rank(List.of(weakerSemantic, strongerSemantic));

        assertThat(result)
                .extracting(candidate -> candidate.place().slug())
                .containsExactly("stronger", "weaker");
    }

    @Test
    void usesShorterTravelTimeWhenFinalAndSemanticScoresTie() {
        ScoredCandidate slower = candidate("slower", 0.90, 30, 0.90);
        ScoredCandidate faster = candidate("faster", 0.90, 10, 0.90);

        List<ScoredCandidate> result = ranker.rank(List.of(slower, faster));

        assertThat(result).extracting(candidate -> candidate.place().slug()).containsExactly("faster", "slower");
    }

    @Test
    void usesSlugAsDeterministicFinalTieBreaker() {
        ScoredCandidate placeB = candidate("place-b", 0.90, 10, 0.90);
        ScoredCandidate placeA = candidate("place-a", 0.90, 10, 0.90);

        List<ScoredCandidate> result = ranker.rank(List.of(placeB, placeA));

        assertThat(result).extracting(candidate -> candidate.place().slug()).containsExactly("place-a", "place-b");
    }

    private ScoredCandidate candidate(String slug, double semanticScore, int travelMinutes, double finalScore) {
        SchedulingPlace place = new SchedulingPlace(
                slug,
                slug,
                BigDecimal.ONE,
                BigDecimal.ONE,
                90,
                BigDecimal.ZERO,
                true,
                null,
                OpeningWindow.unknown());

        return new ScoredCandidate(
                place, "HIGHLIGHTS", semanticScore, 2.0, travelMinutes, 0.80, 0.80, 1.00, finalScore);
    }
}
