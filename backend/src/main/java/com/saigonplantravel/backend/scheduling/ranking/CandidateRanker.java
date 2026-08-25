package com.saigonplantravel.backend.scheduling.ranking;

import com.saigonplantravel.backend.scheduling.model.ScoredCandidate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CandidateRanker {

    private static final Comparator<ScoredCandidate> ORDER = Comparator.comparingDouble(ScoredCandidate::finalScore)
            .reversed()
            .thenComparing(
                    Comparator.comparingDouble(ScoredCandidate::semanticScore).reversed())
            .thenComparingInt(ScoredCandidate::travelMinutes)
            .thenComparing(candidate -> candidate.place().getSlug());

    public List<ScoredCandidate> rank(List<ScoredCandidate> candidates) {

        return candidates.stream().sorted(ORDER).toList();
    }
}
