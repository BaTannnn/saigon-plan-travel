package com.saigonplantravel.backend.trip.domain;

import com.saigonplantravel.backend.trip.exception.InvalidTripException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TripPolicy {

    private static final long MIN_DURATION_MINUTES = 60;
    private static final long MAX_DURATION_MINUTES = 18 * 60;

    private final Clock clock;

    public TripPolicy(Clock clock) {
        this.clock = clock;
    }

    public void validate(LocalDate tripDate, LocalTime startTime, LocalTime endTime, List<String> categorySlugs) {
        validateTripDate(tripDate);
        validateTimeWindow(startTime, endTime);
        validateCategoryDuplicates(categorySlugs);
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

    private void validateCategoryDuplicates(List<String> categorySlugs) {
        Set<String> uniqueSlugs = new HashSet<>();

        for (String slug : categorySlugs) {
            boolean added = uniqueSlugs.add(slug);

            if (!added) {
                throw new InvalidTripException(
                        "DUPLICATE_CATEGORY_PREFERENCE",
                        "categorySlugs",
                        "category preferences must not contain duplicates");
            }
        }
    }
}
