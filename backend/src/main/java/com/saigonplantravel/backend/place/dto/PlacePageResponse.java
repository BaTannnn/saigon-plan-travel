package com.saigonplantravel.backend.place.dto;

import java.util.List;

public record PlacePageResponse(
        List<PlaceSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
