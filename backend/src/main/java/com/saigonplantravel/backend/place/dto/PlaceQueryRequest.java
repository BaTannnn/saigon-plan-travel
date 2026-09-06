package com.saigonplantravel.backend.place.dto;

import com.saigonplantravel.backend.place.search.PlaceSearchNormalizer;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PlaceQueryRequest(
        @Size(max = 100, message = "keyword must contain at most 100 characters") String keyword,
        @Size(max = 120, message = "category must contain at most 120 characters")
                @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "category must be a lowercase slug")
                String category,
        Boolean indoor,
        @DecimalMin(value = "0.00", message = "maxCost must be greater than or equal to 0")
                @DecimalMax(value = "100000000.00", message = "maxCost must be less than or equal to 100000000")
                BigDecimal maxCost,
        @Min(value = 0, message = "page must be greater than or equal to 0") Integer page,
        @Min(value = 1, message = "size must be between 1 and 100")
                @Max(value = 100, message = "size must be between 1 and 100")
                Integer size) {

    public PlaceQueryRequest {
        keyword = PlaceSearchNormalizer.normalizeText(keyword);
    }

    public int resolvedPage() {
        return page == null ? 0 : page;
    }

    public int resolvedSize() {
        return size == null ? 20 : size;
    }
}
