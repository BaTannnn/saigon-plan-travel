package com.saigonplantravel.backend.scheduling.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(
        prefix = "scheduling"
)
public record SchedulingProperties(

        @NotNull
        @Pattern(
                regexp = "GREEDY_V1",
                message =
                        "algorithmVersion must be GREEDY_V1"
        )
        String algorithmVersion,

        @NotNull
        @Positive(
                message =
                        "averageSpeedKmh must be positive"
        )
        BigDecimal averageSpeedKmh,

        @PositiveOrZero(
                message =
                        "fixedTransferMinutes must not be negative"
        )
        int fixedTransferMinutes,

        @Min(
                value = 1,
                message =
                        "maxCandidates must be at least 1"
        )
        @Max(
                value = 100,
                message =
                        "maxCandidates must not exceed 100"
        )
        int maxCandidates

) {
}