package com.saigonplantravel.backend.trip.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saigonplantravel.backend.trip.exception.InvalidTripException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TripPolicyTest {

    private TripPolicy policy;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-31T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        policy = new TripPolicy(fixedClock);
    }

    @Test
    void shouldAcceptValidTripDraft() {
        assertThatCode(() -> policy.validate(LocalDate.of(2026, 7, 31), LocalTime.of(8, 0), LocalTime.of(18, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectPastTripDate() {
        assertThatThrownBy(() -> policy.validate(LocalDate.of(2026, 7, 30), LocalTime.of(8, 0), LocalTime.of(18, 0)))
                .isInstanceOf(InvalidTripException.class)
                .hasMessageContaining("trip date must be today or in the future");
    }

    @Test
    void shouldRejectInvalidTimeOrder() {
        assertThatThrownBy(() -> policy.validate(LocalDate.of(2026, 8, 1), LocalTime.of(18, 0), LocalTime.of(8, 0)))
                .isInstanceOf(InvalidTripException.class)
                .hasMessageContaining("end time must be after start time");
    }

    @Test
    void shouldRejectDurationShorterThanOneHour() {
        assertThatThrownBy(() -> policy.validate(LocalDate.of(2026, 8, 1), LocalTime.of(8, 0), LocalTime.of(8, 59)))
                .isInstanceOf(InvalidTripException.class)
                .hasMessageContaining("trip duration must be between");
    }

    @Test
    void shouldRejectDurationLongerThanEighteenHours() {
        assertThatThrownBy(() -> policy.validate(LocalDate.of(2026, 8, 1), LocalTime.of(4, 0), LocalTime.of(22, 1)))
                .isInstanceOf(InvalidTripException.class)
                .hasMessageContaining("trip duration must be between");
    }
}
