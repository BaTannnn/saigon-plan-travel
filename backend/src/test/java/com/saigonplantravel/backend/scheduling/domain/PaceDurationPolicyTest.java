package com.saigonplantravel.backend.scheduling.domain;

import com.saigonplantravel.backend.trip.domain.TravelPace;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaceDurationPolicyTest {

    private final PaceDurationPolicy policy =
            new PaceDurationPolicy();

    @Test
    void adjustsDurationByTravelPaceAndRoundsUpToFiveMinutes() {
        int relaxedMinutes =
                policy.adjustVisitMinutes(
                        61,
                        TravelPace.RELAXED
                );

        int balancedMinutes =
                policy.adjustVisitMinutes(
                        61,
                        TravelPace.BALANCED
                );

        int fastMinutes =
                policy.adjustVisitMinutes(
                        61,
                        TravelPace.FAST
                );

        assertThat(relaxedMinutes)
                .isEqualTo(80);

        assertThat(balancedMinutes)
                .isEqualTo(65);

        assertThat(fastMinutes)
                .isEqualTo(50);
    }
}