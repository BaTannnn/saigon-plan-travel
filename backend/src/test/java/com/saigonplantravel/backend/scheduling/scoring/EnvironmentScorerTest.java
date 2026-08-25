package com.saigonplantravel.backend.scheduling.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import org.junit.jupiter.api.Test;

class EnvironmentScorerTest {

    private final EnvironmentScorer scorer = new EnvironmentScorer();

    @Test
    void indoorPreferenceRewardsIndoorPlace() {

        assertThat(scorer.score(EnvironmentPreference.INDOOR, true)).isEqualTo(1.0);

        assertThat(scorer.score(EnvironmentPreference.INDOOR, false)).isEqualTo(0.0);
    }

    @Test
    void outdoorPreferenceRewardsOutdoorPlace() {

        assertThat(scorer.score(EnvironmentPreference.OUTDOOR, false)).isEqualTo(1.0);

        assertThat(scorer.score(EnvironmentPreference.OUTDOOR, true)).isEqualTo(0.0);
    }

    @Test
    void mixedPreferenceDoesNotFavorEitherEnvironment() {

        assertThat(scorer.score(EnvironmentPreference.MIXED, true)).isEqualTo(1.0);

        assertThat(scorer.score(EnvironmentPreference.MIXED, false)).isEqualTo(1.0);
    }
}
