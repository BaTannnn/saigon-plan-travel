package com.saigonplantravel.backend.trip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StartLocationRequest(

        @NotBlank
        @Size(max = 255)
        String label,

        @NotNull
        @Digits(integer = 3, fraction = 7)
        @DecimalMin("-90.0000000")
        @DecimalMax("90.0000000")
        BigDecimal latitude,

        @NotNull
        @Digits(integer = 3, fraction = 7)
        @DecimalMin("-180.0000000")
        @DecimalMax("180.0000000")
        BigDecimal longitude

) {

    public StartLocationRequest {
        if (label != null) {
            label = label.trim();
        }
    }
}