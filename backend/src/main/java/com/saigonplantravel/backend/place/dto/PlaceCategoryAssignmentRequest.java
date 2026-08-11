package com.saigonplantravel.backend.place.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PlaceCategoryAssignmentRequest(
        List<@NotBlank @Size(max = 120) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") String> categorySlugs) {

    public PlaceCategoryAssignmentRequest {
        categorySlugs = categorySlugs == null
                ? List.of()
                : categorySlugs.stream()
                        .map(slug -> slug == null ? null : slug.trim())
                        .distinct()
                        .toList();
    }
}
