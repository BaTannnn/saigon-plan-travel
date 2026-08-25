package com.saigonplantravel.backend.recommendation.filter;

import com.saigonplantravel.backend.place.entity.OpeningHour;
import com.saigonplantravel.backend.place.entity.Place;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OpeningHoursFeasibilityEvaluator {

    public OpeningHoursFeasibility evaluate(Place place, LocalDate tripDate, LocalTime tripStart, LocalTime tripEnd) {
        List<OpeningHour> openingHours = openingHoursFor(place, tripDate);

        if (openingHours.isEmpty()) {
            return OpeningHoursFeasibility.UNKNOWN;
        }

        boolean hasUnknownInterval = false;

        for (OpeningHour openingHour : openingHours) {
            if (Boolean.TRUE.equals(openingHour.getClosed())) {
                continue;
            }

            if (!isCompleteOpenInterval(openingHour)) {
                hasUnknownInterval = true;
                continue;
            }

            LocalTime availableStart = laterOf(tripStart, openingHour.getOpenTime());
            LocalTime availableEnd = earlierOf(tripEnd, openingHour.getCloseTime());

            if (availableStart.isBefore(availableEnd)) {
                long availableMinutes =
                        Duration.between(availableStart, availableEnd).toMinutes();

                if (availableMinutes >= place.getEstimatedVisitMinutes()) {
                    return OpeningHoursFeasibility.FEASIBLE;
                }
            }
        }

        return hasUnknownInterval ? OpeningHoursFeasibility.UNKNOWN : OpeningHoursFeasibility.INFEASIBLE;
    }

    public OpeningHoursFeasibility evaluatePossibleOverlap(
            Place place, LocalDate tripDate, LocalTime tripStart, LocalTime tripEnd) {
        List<OpeningHour> openingHours = openingHoursFor(place, tripDate);

        if (openingHours.isEmpty()) {
            return OpeningHoursFeasibility.UNKNOWN;
        }

        boolean hasUnknownInterval = false;

        for (OpeningHour openingHour : openingHours) {
            if (Boolean.TRUE.equals(openingHour.getClosed())) {
                continue;
            }

            if (!isCompleteOpenInterval(openingHour)) {
                hasUnknownInterval = true;
                continue;
            }

            LocalTime availableStart = laterOf(tripStart, openingHour.getOpenTime());
            LocalTime availableEnd = earlierOf(tripEnd, openingHour.getCloseTime());

            if (availableStart.isBefore(availableEnd)) {
                return OpeningHoursFeasibility.FEASIBLE;
            }
        }

        return hasUnknownInterval ? OpeningHoursFeasibility.UNKNOWN : OpeningHoursFeasibility.INFEASIBLE;
    }

    private List<OpeningHour> openingHoursFor(Place place, LocalDate tripDate) {
        short dayOfWeek = (short) tripDate.getDayOfWeek().getValue();

        return place.getOpeningHours().stream()
                .filter(hour -> hour.getDayOfWeek() == dayOfWeek)
                .toList();
    }

    private boolean isCompleteOpenInterval(OpeningHour openingHour) {
        return Boolean.FALSE.equals(openingHour.getClosed())
                && openingHour.getOpenTime() != null
                && openingHour.getCloseTime() != null;
    }

    private LocalTime laterOf(LocalTime first, LocalTime second) {

        return first.isAfter(second) ? first : second;
    }

    private LocalTime earlierOf(LocalTime first, LocalTime second) {

        return first.isBefore(second) ? first : second;
    }
}
