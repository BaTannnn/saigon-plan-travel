package com.saigonplantravel.backend.trip.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.dto.TripResponse;
import com.saigonplantravel.backend.trip.dto.TripSummaryResponse;
import com.saigonplantravel.backend.trip.entity.Trip;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class TripMapperTest {

    private final TripMapper mapper = new TripMapper();

    @Test
    void mapsTripToDraftResponseWithoutInternalIdentifiers() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-01T10:00:00+07:00");

        Trip trip = new Trip(
                99L,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                "Chợ Bến Thành",
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                createdAt);

        TripResponse response = mapper.toResponse(trip);

        assertThat(response.publicId()).isEqualTo(trip.getPublicId());

        assertThat(response.tripDate()).isEqualTo(LocalDate.of(2026, 8, 20));

        assertThat(response.startTime()).isEqualTo(LocalTime.of(8, 0));

        assertThat(response.endTime()).isEqualTo(LocalTime.of(18, 0));

        assertThat(response.budget()).isEqualByComparingTo("500000.00");

        assertThat(response.startLocation().label()).isEqualTo("Chợ Bến Thành");

        assertThat(response.startLocation().latitude()).isEqualByComparingTo("10.7726400");

        assertThat(response.startLocation().longitude()).isEqualByComparingTo("106.6980500");

        assertThat(response.travelPace()).isEqualTo(TravelPace.BALANCED);

        assertThat(response.environmentPreference()).isEqualTo(EnvironmentPreference.MIXED);

        assertThat(response.createdAt()).isEqualTo(createdAt);

        assertThat(response.updatedAt()).isEqualTo(createdAt);

        List<String> responseFields = Arrays.stream(TripResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(responseFields).doesNotContain("id", "userId");
    }

    @Test
    void mapsTripToLightweightSummaryWithoutInternalOrCoordinateFields() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-01T10:00:00+07:00");

        Trip trip = new Trip(
                99L,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                "Chợ Bến Thành",
                new BigDecimal("10.7726400"),
                new BigDecimal("106.6980500"),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                timestamp);

        TripSummaryResponse response = mapper.toSummaryResponse(trip);

        assertThat(response.publicId()).isEqualTo(trip.getPublicId());
        assertThat(response.startLocationLabel()).isEqualTo("Chợ Bến Thành");
        assertThat(response.updatedAt()).isEqualTo(timestamp);

        List<String> responseFields = Arrays.stream(TripSummaryResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(responseFields).doesNotContain("id", "userId", "startLatitude", "startLongitude", "createdAt");
    }
}
