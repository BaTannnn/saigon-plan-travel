package com.saigonplantravel.backend.place.mapper;

import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.dto.OpeningHourResponse;
import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.PlaceSummaryResponse;
import com.saigonplantravel.backend.place.entity.Category;
import com.saigonplantravel.backend.place.entity.OpeningHour;
import com.saigonplantravel.backend.place.entity.Place;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlaceMapper {

    private static final Comparator<Category> CATEGORY_ORDER =
            Comparator.comparing(Category::getName).thenComparing(Category::getId);

    private static final Comparator<OpeningHour> OPENING_HOUR_ORDER = Comparator.comparing(OpeningHour::getDayOfWeek);

    public PlaceSummaryResponse toSummaryResponse(Place place) {
        return new PlaceSummaryResponse(
                place.getId(),
                place.getName(),
                place.getSlug(),
                place.getShortDescription(),
                place.getLatitude(),
                place.getLongitude(),
                place.getEstimatedVisitMinutes(),
                place.getMinCost(),
                place.getMaxCost(),
                place.getIndoor());
    }

    public PlaceDetailResponse toDetailResponse(Place place) {
        List<CategoryResponse> categories = place.getCategories().stream()
                .sorted(CATEGORY_ORDER)
                .map(this::toCategoryResponse)
                .toList();

        List<OpeningHourResponse> openingHours = place.getOpeningHours().stream()
                .sorted(OPENING_HOUR_ORDER)
                .map(this::toOpeningHourResponse)
                .toList();

        return new PlaceDetailResponse(
                place.getId(),
                place.getName(),
                place.getSlug(),
                place.getShortDescription(),
                place.getFullDescription(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getEstimatedVisitMinutes(),
                place.getMinCost(),
                place.getMaxCost(),
                place.getIndoor(),
                categories,
                openingHours);
    }

    public CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }

    private OpeningHourResponse toOpeningHourResponse(OpeningHour openingHour) {
        return new OpeningHourResponse(
                openingHour.getDayOfWeek(),
                openingHour.getClosed(),
                openingHour.getOpenTime(),
                openingHour.getCloseTime());
    }
}
