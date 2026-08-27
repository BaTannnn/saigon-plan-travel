package com.saigonplantravel.backend.recommendation;

import com.saigonplantravel.backend.recommendation.model.SemanticPlaceCandidate;
import java.util.List;

public interface SemanticPlaceRetriever {

    List<SemanticPlaceCandidate> retrieve(
            String preferenceDescription, int limit, List<String> eligiblePlaceSlugs);
}
