package com.saigonplantravel.backend.trip.dto;

import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record SaveTripRequest(

        @NotNull
        LocalDate tripDate,

        @NotNull
        LocalTime startTime,

        @NotNull
        LocalTime endTime,

        @NotNull
        @Digits(integer = 9, fraction = 2)
        @DecimalMin("0.00")
        @DecimalMax("100000000.00")
        BigDecimal budget,

        @NotNull
        @Valid
        StartLocationRequest startLocation,

        @NotNull
        TravelPace travelPace,

        @NotNull
        EnvironmentPreference environmentPreference,

        @NotNull
        @Size(min = 1, max = 5)
        List<
                @NotBlank
                @Size(max = 120)
                @Pattern(
                        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$"
                )
                        String
                > categorySlugs

) {

    public SaveTripRequest {
        if (categorySlugs != null) {
            List<String> normalizedSlugs =
                    new ArrayList<>(categorySlugs.size());

            for (String slug : categorySlugs) {
                normalizedSlugs.add(
                        slug == null
                                ? null
                                : slug.trim()
                );
            }

            categorySlugs = Collections.unmodifiableList(
                    normalizedSlugs
            );
        }
    }
}