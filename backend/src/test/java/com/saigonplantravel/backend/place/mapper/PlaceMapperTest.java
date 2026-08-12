package com.saigonplantravel.backend.place.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.place.dto.admin.AdminPlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.admin.AdminPlaceSummaryResponse;
import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.dto.OpeningHourResponse;
import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.PlaceSummaryResponse;
import com.saigonplantravel.backend.place.entity.Category;
import com.saigonplantravel.backend.place.entity.OpeningHour;
import com.saigonplantravel.backend.place.entity.Place;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlaceMapperTest {

    @Test
    void mapsAllSummaryFieldsIncludingNullDescription() {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(1L);
        when(place.getName()).thenReturn("Demo Place");
        when(place.getSlug()).thenReturn("demo-place");
        when(place.getShortDescription()).thenReturn(null);
        when(place.getLatitude()).thenReturn(new BigDecimal("10.0000000"));
        when(place.getLongitude()).thenReturn(new BigDecimal("106.0000000"));
        when(place.getEstimatedVisitMinutes()).thenReturn(60);
        when(place.getMinCost()).thenReturn(BigDecimal.ZERO);
        when(place.getMaxCost()).thenReturn(new BigDecimal("100000.00"));
        when(place.getIndoor()).thenReturn(true);
        when(place.getActive()).thenReturn(false);

        PlaceMapper mapper = new PlaceMapper();
        PlaceSummaryResponse response = mapper.toSummaryResponse(place);
        AdminPlaceSummaryResponse adminResponse = mapper.toAdminSummaryResponse(place);

        assertThat(response)
                .isEqualTo(new PlaceSummaryResponse(
                        1L,
                        "Demo Place",
                        "demo-place",
                        null,
                        new BigDecimal("10.0000000"),
                        new BigDecimal("106.0000000"),
                        60,
                        BigDecimal.ZERO,
                        new BigDecimal("100000.00"),
                        true));
        assertThat(adminResponse.active()).isFalse();
        assertThat(adminResponse.slug()).isEqualTo(response.slug());
    }

    @Test
    void mapsAndSortsLockedDetailContract() {
        Place place = mock(Place.class);
        Category culture = category(2L, "Văn hóa", "van-hoa");
        Category art = category(1L, "Nghệ thuật", "nghe-thuat");
        OpeningHour tuesdayClosed = openingHour((short) 2, true, null, null);
        OpeningHour mondayOpen = openingHour((short) 1, false, LocalTime.of(9, 0), LocalTime.of(17, 0));

        when(place.getId()).thenReturn(1L);
        when(place.getName()).thenReturn("Demo Art Space");
        when(place.getSlug()).thenReturn("demo-art-space");
        when(place.getShortDescription()).thenReturn(null);
        when(place.getFullDescription()).thenReturn(null);
        when(place.getAddress()).thenReturn("Địa chỉ demo 1");
        when(place.getLatitude()).thenReturn(new BigDecimal("10.7750000"));
        when(place.getLongitude()).thenReturn(new BigDecimal("106.7000000"));
        when(place.getEstimatedVisitMinutes()).thenReturn(90);
        when(place.getMinCost()).thenReturn(new BigDecimal("50000.00"));
        when(place.getMaxCost()).thenReturn(new BigDecimal("150000.00"));
        when(place.getIndoor()).thenReturn(true);
        when(place.getCategories()).thenReturn(Set.of(culture, art));
        when(place.getOpeningHours()).thenReturn(List.of(tuesdayClosed, mondayOpen));
        when(place.getActive()).thenReturn(false);

        PlaceMapper mapper = new PlaceMapper();
        PlaceDetailResponse response = mapper.toDetailResponse(place);
        AdminPlaceDetailResponse adminResponse = mapper.toAdminDetailResponse(place);

        assertThat(response.categories())
                .containsExactly(
                        new CategoryResponse(1L, "Nghệ thuật", "nghe-thuat"),
                        new CategoryResponse(2L, "Văn hóa", "van-hoa"));
        assertThat(response.openingHours())
                .containsExactly(
                        new OpeningHourResponse((short) 1, false, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                        new OpeningHourResponse((short) 2, true, null, null));
        assertThat(response.shortDescription()).isNull();
        assertThat(response.fullDescription()).isNull();
        assertThat(response.address()).isEqualTo("Địa chỉ demo 1");
        assertThat(adminResponse.active()).isFalse();
        assertThat(adminResponse.categories()).isEqualTo(response.categories());
        assertThat(adminResponse.openingHours()).isEqualTo(response.openingHours());
    }

    private Category category(Long id, String name, String slug) {
        Category category = mock(Category.class);
        when(category.getId()).thenReturn(id);
        when(category.getName()).thenReturn(name);
        when(category.getSlug()).thenReturn(slug);
        return category;
    }

    private OpeningHour openingHour(short dayOfWeek, boolean closed, LocalTime openTime, LocalTime closeTime) {
        OpeningHour openingHour = mock(OpeningHour.class);
        when(openingHour.getDayOfWeek()).thenReturn(dayOfWeek);
        when(openingHour.getClosed()).thenReturn(closed);
        when(openingHour.getOpenTime()).thenReturn(openTime);
        when(openingHour.getCloseTime()).thenReturn(closeTime);
        return openingHour;
    }
}
