package com.saigonplantravel.backend.place.scheduling;

import com.saigonplantravel.backend.place.domain.AdministrativeUnitType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceSchedulingCandidateTest {

    @Test
    void createsImmutableCandidateWithSortedCategoryIds() {
        Set<Long> categoryIds =
                new HashSet<>(
                        Set.of(5L, 2L)
                );

        PlaceSchedulingCandidate candidate =
                validCandidate(categoryIds);

        assertThat(
                candidate.matchedPreferredCategoryIds()
        ).containsExactly(
                2L,
                5L
        );

        categoryIds.add(9L);

        assertThat(
                candidate.matchedPreferredCategoryIds()
        ).containsExactly(
                2L,
                5L
        );

        assertThatThrownBy(
                () -> candidate
                        .matchedPreferredCategoryIds()
                        .add(10L)
        ).isInstanceOf(
                UnsupportedOperationException.class
        );
    }

    @Test
    void rejectsCandidateWithoutMatchedCategory() {
        assertThatThrownBy(
                () -> validCandidate(Set.of())
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "matchedPreferredCategoryIds must not be empty"
                );
    }

    @Test
    void preservesUnknownAdministrativeUnitAsNullPair() {
        PlaceSchedulingCandidate candidate =
                new PlaceSchedulingCandidate(
                        10L,
                        "Bảo tàng Demo",
                        "bao-tang-demo",
                        "Địa chỉ demo, TP.HCM",
                        null,
                        null,
                        new BigDecimal("10.7768890"),
                        new BigDecimal("106.7008060"),
                        90,
                        new BigDecimal("50000.00"),
                        true,
                        Set.of(1L),
                        OpeningHoursSnapshot.unknown()
                );

        assertThat(candidate.administrativeUnitName()).isNull();
        assertThat(candidate.administrativeUnitType()).isNull();
    }

    @Test
    void rejectsIncompleteAdministrativeUnitPair() {
        assertThatThrownBy(
                () -> new PlaceSchedulingCandidate(
                        10L,
                        "Bảo tàng Demo",
                        "bao-tang-demo",
                        "Địa chỉ demo, TP.HCM",
                        "Bến Nghé",
                        null,
                        new BigDecimal("10.7768890"),
                        new BigDecimal("106.7008060"),
                        90,
                        new BigDecimal("50000.00"),
                        true,
                        Set.of(1L),
                        OpeningHoursSnapshot.unknown()
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidSchedulingValues() {
        assertThatThrownBy(
                () -> new PlaceSchedulingCandidate(
                        1L,
                        "Place",
                        "place",
                        "Address",
                        "Bến Nghé",
                        AdministrativeUnitType.WARD,
                        new BigDecimal("91"),
                        new BigDecimal("106.7"),
                        90,
                        BigDecimal.ZERO,
                        true,
                        Set.of(1L),
                        OpeningHoursSnapshot.unknown()
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );

        assertThatThrownBy(
                () -> new PlaceSchedulingCandidate(
                        1L,
                        "Place",
                        "place",
                        "Address",
                        "Bến Nghé",
                        AdministrativeUnitType.WARD,
                        new BigDecimal("10.7"),
                        new BigDecimal("106.7"),
                        0,
                        BigDecimal.ZERO,
                        true,
                        Set.of(1L),
                        OpeningHoursSnapshot.unknown()
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );

        assertThatThrownBy(
                () -> new PlaceSchedulingCandidate(
                        1L,
                        "Place",
                        "place",
                        "Address",
                        "Bến Nghé",
                        AdministrativeUnitType.WARD,
                        new BigDecimal("10.7"),
                        new BigDecimal("106.7"),
                        90,
                        new BigDecimal("-1"),
                        true,
                        Set.of(1L),
                        OpeningHoursSnapshot.unknown()
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }

    private PlaceSchedulingCandidate validCandidate(
            Set<Long> categoryIds
    ) {
        return new PlaceSchedulingCandidate(
                10L,
                "Bảo tàng Demo",
                "bao-tang-demo",
                "Địa chỉ demo, TP.HCM",
                "Bến Nghé",
                AdministrativeUnitType.WARD,
                new BigDecimal("10.7768890"),
                new BigDecimal("106.7008060"),
                90,
                new BigDecimal("50000.00"),
                true,
                categoryIds,
                OpeningHoursSnapshot.knownOpen(
                        java.time.LocalTime.of(8, 0),
                        java.time.LocalTime.of(17, 0)
                )
        );
    }
}
