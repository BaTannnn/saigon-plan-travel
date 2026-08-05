package com.saigonplantravel.backend.place.scheduling;

import com.saigonplantravel.backend.place.scheduling.PlaceSchedulingCandidate;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface PlaceSchedulingQuery {

    List<PlaceSchedulingCandidate> findCandidates(
            Set<Long> preferredCategoryIds,
            LocalDate tripDate
    );
}