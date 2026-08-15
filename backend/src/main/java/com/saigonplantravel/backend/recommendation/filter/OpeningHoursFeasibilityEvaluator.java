package com.saigonplantravel.backend.recommendation.filter;

import com.saigonplantravel.backend.place.entity.OpeningHour;
import com.saigonplantravel.backend.place.entity.Place;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

@Component
public class OpeningHoursFeasibilityEvaluator {

    public OpeningHoursFeasibility evaluate(Place place, LocalDate tripDate, LocalTime tripStart, LocalTime tripEnd) {

        short dayOfWeek = (short) tripDate.getDayOfWeek().getValue();

        OpeningHour openingHour = place.getOpeningHours().stream()
                .filter(hour -> hour.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElse(null);

        if (openingHour == null) {
            return OpeningHoursFeasibility.UNKNOWN;
        }

        if (Boolean.TRUE.equals(openingHour.getClosed())) {
            return OpeningHoursFeasibility.INFEASIBLE;
        }

        LocalTime availableStart = laterOf(tripStart, openingHour.getOpenTime());

        LocalTime availableEnd = earlierOf(tripEnd, openingHour.getCloseTime());

        if (!availableStart.isBefore(availableEnd)) {
            return OpeningHoursFeasibility.INFEASIBLE;
        }

        long availableMinutes = Duration.between(availableStart, availableEnd).toMinutes();

        return availableMinutes >= place.getEstimatedVisitMinutes()
                ? OpeningHoursFeasibility.FEASIBLE
                : OpeningHoursFeasibility.INFEASIBLE;
    }

    private LocalTime laterOf(LocalTime first, LocalTime second) {

        return first.isAfter(second) ? first : second;
    }

    private LocalTime earlierOf(LocalTime first, LocalTime second) {

        return first.isBefore(second) ? first : second;
    }
}
