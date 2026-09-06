package com.saigonplantravel.backend.place.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PlaceCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank
                @Size(max = 180)
                @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "must use lowercase kebab-case")
                String slug,
        @Size(max = 500) String shortDescription,
        String fullDescription,
        @NotBlank @Size(max = 255) String address,
        @NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 7) BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 7) BigDecimal longitude,
        @NotNull @Positive Integer estimatedVisitMinutes,
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal minCost,
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal maxCost,
        @NotNull Boolean indoor) {

    public PlaceCreateRequest {
        name = normalizeRequired(name);
        slug = normalizeRequired(slug);
        shortDescription = normalizeOptional(shortDescription);
        fullDescription = normalizeOptional(fullDescription);
        address = normalizeRequired(address);
    }

    public boolean hasInvalidCostRange() {
        return minCost != null && maxCost != null && maxCost.compareTo(minCost) < 0;
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
