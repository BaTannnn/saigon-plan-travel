package com.saigonplantravel.backend.place.scheduling;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpeningHoursSnapshotTest {

    @Test
    void createsKnownOpenSnapshot() {
        OpeningHoursSnapshot snapshot =
                OpeningHoursSnapshot.knownOpen(
                        LocalTime.of(8, 0),
                        LocalTime.of(17, 0)
                );

        assertThat(snapshot.status())
                .isEqualTo(
                        OpeningHoursStatus.KNOWN_OPEN
                );

        assertThat(snapshot.openTime())
                .isEqualTo(
                        LocalTime.of(8, 0)
                );

        assertThat(snapshot.closeTime())
                .isEqualTo(
                        LocalTime.of(17, 0)
                );
    }

    @Test
    void createsClosedAndUnknownWithoutIntervals() {
        OpeningHoursSnapshot closed =
                OpeningHoursSnapshot.closed();

        OpeningHoursSnapshot unknown =
                OpeningHoursSnapshot.unknown();

        assertThat(closed.status())
                .isEqualTo(
                        OpeningHoursStatus.CLOSED
                );

        assertThat(closed.openTime())
                .isNull();

        assertThat(closed.closeTime())
                .isNull();

        assertThat(unknown.status())
                .isEqualTo(
                        OpeningHoursStatus.UNKNOWN
                );

        assertThat(unknown.openTime())
                .isNull();

        assertThat(unknown.closeTime())
                .isNull();
    }

    @Test
    void rejectsKnownOpenWithoutValidInterval() {
        assertThatThrownBy(
                () -> new OpeningHoursSnapshot(
                        OpeningHoursStatus.KNOWN_OPEN,
                        null,
                        LocalTime.of(17, 0)
                )
        ).isInstanceOf(
                NullPointerException.class
        );

        assertThatThrownBy(
                () -> OpeningHoursSnapshot.knownOpen(
                        LocalTime.of(17, 0),
                        LocalTime.of(8, 0)
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "openTime must be before closeTime"
                );
    }

    @Test
    void rejectsIntervalForClosedOrUnknownStatus() {
        assertThatThrownBy(
                () -> new OpeningHoursSnapshot(
                        OpeningHoursStatus.UNKNOWN,
                        LocalTime.of(8, 0),
                        LocalTime.of(17, 0)
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );

        assertThatThrownBy(
                () -> new OpeningHoursSnapshot(
                        OpeningHoursStatus.CLOSED,
                        LocalTime.of(8, 0),
                        LocalTime.of(17, 0)
                )
        ).isInstanceOf(
                IllegalArgumentException.class
        );
    }
}