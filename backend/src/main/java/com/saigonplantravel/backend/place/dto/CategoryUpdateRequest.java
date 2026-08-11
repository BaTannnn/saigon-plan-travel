package com.saigonplantravel.backend.place.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(@NotBlank @Size(max = 100) String name, String description) {
    public CategoryUpdateRequest {
        name = normalizeRequired(name);
        description = normalizeOptional(description);
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = normalizeRequired(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }
}
