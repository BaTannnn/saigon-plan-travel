package com.saigonplantravel.backend.place.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") String slug,
        String description) {

    public CategoryCreateRequest {
        name = normalizeRequired(name);
        slug = normalizeRequired(slug);
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
