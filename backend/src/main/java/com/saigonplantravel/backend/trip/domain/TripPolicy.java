package com.saigonplantravel.backend.trip.domain;

import com.saigonplantravel.backend.trip.exception.InvalidTripException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

@Component
public class TripPolicy {

    private static final long MIN_DURATION_MINUTES = 60;
    private static final long MAX_DURATION_MINUTES = 18 * 60;
    private static final int MAX_TRIPS_PER_DAY = 5;

    private final Clock clock;

    public TripPolicy(Clock clock) {
        this.clock = clock;
    }

    public void validate(LocalDate tripDate, LocalTime startTime, LocalTime endTime) {
        validateTripDate(tripDate);
        validateTimeWindow(startTime, endTime);
    }

    public void validateDailyTripLimit(long existingTripCount) {
        if (existingTripCount >= MAX_TRIPS_PER_DAY) {
            throw new InvalidTripException(
                    "TRIP_DAILY_LIMIT_EXCEEDED", "tripDate", "You can create at most 5 trips on the same day");
        }
    }

    private void validateTripDate(LocalDate tripDate) {
        LocalDate today = LocalDate.now(clock);

        if (tripDate.isBefore(today)) {
            throw new InvalidTripException("TRIP_DATE_IN_PAST", "tripDate", "trip date must be today or in the future");
        }
    }

    private void validateTimeWindow(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new InvalidTripException("INVALID_TRIP_TIME_ORDER", "endTime", "end time must be after start time");
        }

        long durationMinutes = Duration.between(startTime, endTime).toMinutes();

        if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new InvalidTripException(
                    "INVALID_TRIP_DURATION", "endTime", "trip duration must be between 60 minutes and 18 hours");
        }
    }
}
