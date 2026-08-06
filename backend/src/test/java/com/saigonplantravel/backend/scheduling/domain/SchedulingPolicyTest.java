package com.saigonplantravel.backend.scheduling.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchedulingPolicyTest {

    @Test
    void createsValidGreedyV1Policy() {
        SchedulingPolicy policy =
                new SchedulingPolicy(
                        SchedulingAlgorithmVersion.GREEDY_V1,
                        new BigDecimal("18.00"),
                        5,
                        100
                );

        assertThat(policy.algorithmVersion())
                .isEqualTo(
                        SchedulingAlgorithmVersion.GREEDY_V1
                );

        assertThat(policy.averageSpeedKmh())
                .isEqualByComparingTo(
                        "18.00"
                );

        assertThat(policy.fixedTransferMinutes())
                .isEqualTo(5);

        assertThat(policy.maxCandidates())
                .isEqualTo(100);
    }

    @Test
    void rejectsInvalidPolicyValues() {
        assertThatThrownBy(
                () -> new SchedulingPolicy(
                        SchedulingAlgorithmVersion.GREEDY_V1,
                        BigDecimal.ZERO,
                        5,
                        100
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "averageSpeedKmh must be positive"
                );

        assertThatThrownBy(
                () -> new SchedulingPolicy(
                        SchedulingAlgorithmVersion.GREEDY_V1,
                        new BigDecimal("18.00"),
                        -1,
                        100
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "fixedTransferMinutes must not be negative"
                );

        assertThatThrownBy(
                () -> new SchedulingPolicy(
                        SchedulingAlgorithmVersion.GREEDY_V1,
                        new BigDecimal("18.00"),
                        5,
                        101
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "maxCandidates must be between 1 and 100"
                );
    }
}