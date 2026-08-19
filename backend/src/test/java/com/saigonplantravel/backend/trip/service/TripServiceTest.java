package com.saigonplantravel.backend.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.domain.TripPolicy;
import com.saigonplantravel.backend.trip.dto.SaveTripRequest;
import com.saigonplantravel.backend.trip.dto.StartLocationRequest;
import com.saigonplantravel.backend.trip.dto.StartLocationResponse;
import com.saigonplantravel.backend.trip.dto.TripResponse;
import com.saigonplantravel.backend.trip.dto.TripSummaryResponse;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import com.saigonplantravel.backend.trip.mapper.TripMapper;
import com.saigonplantravel.backend.trip.repository.TripRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripPolicy tripPolicy;

    @Mock
    private TripMapper tripMapper;

    private TripService tripService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-08-01T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        tripService = new TripService(tripRepository, tripPolicy, tripMapper, fixedClock);
    }

    @Test
    void createsOwnedTripAndMapsResponse() {
        SaveTripRequest request = createValidRequest();
        OffsetDateTime now = OffsetDateTime.parse("2026-08-01T10:00:00+07:00");
        TripResponse expectedResponse = responseFor(UUID.randomUUID(), request, now, now);

        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tripMapper.toResponse(any(Trip.class))).thenReturn(expectedResponse);

        TripResponse response = tripService.createTrip(99L, request);

        assertThat(response).isSameAs(expectedResponse);
        verify(tripPolicy).validate(request.tripDate(), request.startTime(), request.endTime());

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(tripCaptor.capture());
        Trip savedTrip = tripCaptor.getValue();

        assertThat(savedTrip.getUserId()).isEqualTo(99L);
        assertThat(savedTrip.getTripDate()).isEqualTo(request.tripDate());
        assertThat(savedTrip.getStartTime()).isEqualTo(request.startTime());
        assertThat(savedTrip.getEndTime()).isEqualTo(request.endTime());
        assertThat(savedTrip.getBudget()).isEqualByComparingTo(request.budget());
        assertThat(savedTrip.getStartLocationLabel()).isEqualTo("Chợ Bến Thành");
        assertThat(savedTrip.getCreatedAt()).isEqualTo(now);
        assertThat(savedTrip.getUpdatedAt()).isEqualTo(now);
        verify(tripMapper).toResponse(savedTrip);
    }

    @Test
    void getsOwnedTripAndMapsResponse() {
        Long userId = 99L;
        UUID publicId = UUID.randomUUID();
        Trip trip = createTrip(userId, OffsetDateTime.parse("2026-08-01T10:00:00+07:00"));
        TripResponse expectedResponse =
                responseFor(trip.getPublicId(), createValidRequest(), trip.getCreatedAt(), trip.getUpdatedAt());

        when(tripRepository.findByPublicIdAndUserId(publicId, userId)).thenReturn(Optional.of(trip));
        when(tripMapper.toResponse(trip)).thenReturn(expectedResponse);

        assertThat(tripService.getTrip(userId, publicId)).isSameAs(expectedResponse);
        verify(tripRepository).findByPublicIdAndUserId(publicId, userId);
        verify(tripMapper).toResponse(trip);
    }

    @Test
    void listsOwnedTripsInRepositoryOrder() {
        Long userId = 42L;
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-01T10:00:00+07:00");
        Trip firstTrip = createTrip(userId, timestamp);
        Trip secondTrip = new Trip(
                userId,
                LocalDate.of(2026, 8, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                new BigDecimal("700000.00"),
                "Bưu điện Thành phố",
                new BigDecimal("10.7798000"),
                new BigDecimal("106.6990000"),
                TravelPace.RELAXED,
                EnvironmentPreference.INDOOR,
                timestamp);
        TripSummaryResponse firstSummary = summaryFor(firstTrip);
        TripSummaryResponse secondSummary = summaryFor(secondTrip);

        when(tripRepository.findAllByUserIdOrderByTripDateAscStartTimeAscPublicIdAsc(userId))
                .thenReturn(List.of(firstTrip, secondTrip));
        when(tripMapper.toSummaryResponse(firstTrip)).thenReturn(firstSummary);
        when(tripMapper.toSummaryResponse(secondTrip)).thenReturn(secondSummary);

        assertThat(tripService.listTrips(userId)).containsExactly(firstSummary, secondSummary);
    }

    @Test
    void throwsTripNotFoundWhenTripDoesNotBelongToUser() {
        Long userId = 99L;
        UUID publicId = UUID.randomUUID();
        when(tripRepository.findByPublicIdAndUserId(publicId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getTrip(userId, publicId))
                .isInstanceOf(TripNotFoundException.class)
                .hasMessage("Trip not found");

        verify(tripRepository).findByPublicIdAndUserId(publicId, userId);
        verifyNoInteractions(tripMapper);
    }

    @Test
    void replacesOwnedTripWithoutChangingIdentityOrCreatedAt() {
        Long userId = 99L;
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-07-31T09:00:00+07:00");
        Trip trip = createTrip(userId, createdAt);
        UUID publicId = trip.getPublicId();
        SaveTripRequest request = new SaveTripRequest(
                LocalDate.of(2026, 8, 20),
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                new BigDecimal("700000.00"),
                new StartLocationRequest(
                        "Bưu điện Trung tâm Sài Gòn", new BigDecimal("10.7798000"), new BigDecimal("106.6990000")),
                TravelPace.RELAXED,
                EnvironmentPreference.INDOOR);
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-08-01T10:00:00+07:00");
        TripResponse expectedResponse = responseFor(publicId, request, createdAt, updatedAt);

        when(tripRepository.findByPublicIdAndUserId(publicId, userId)).thenReturn(Optional.of(trip));
        when(tripMapper.toResponse(trip)).thenReturn(expectedResponse);

        assertThat(tripService.replaceTrip(userId, publicId, request)).isSameAs(expectedResponse);
        assertThat(trip.getPublicId()).isEqualTo(publicId);
        assertThat(trip.getUserId()).isEqualTo(userId);
        assertThat(trip.getCreatedAt()).isEqualTo(createdAt);
        assertThat(trip.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(trip.getTripDate()).isEqualTo(request.tripDate());
        assertThat(trip.getStartTime()).isEqualTo(request.startTime());
        assertThat(trip.getEndTime()).isEqualTo(request.endTime());
        assertThat(trip.getBudget()).isEqualByComparingTo(request.budget());
        assertThat(trip.getStartLocationLabel()).isEqualTo("Bưu điện Trung tâm Sài Gòn");
        assertThat(trip.getStartLatitude()).isEqualByComparingTo("10.7798000");
        assertThat(trip.getStartLongitude()).isEqualByComparingTo("106.6990000");
        assertThat(trip.getTravelPace()).isEqualTo(TravelPace.RELAXED);
        assertThat(trip.getEnvironmentPreference()).isEqualTo(EnvironmentPreference.INDOOR);
        verify(tripPolicy).validate(request.tripDate(), request.startTime(), request.endTime());
        verify(tripRepository, never()).save(any(Trip.class));
        verify(tripMapper).toResponse(trip);
    }

    @Test
    void throwsTripNotFoundBeforeReplacingUnownedTrip() {
        Long userId = 99L;
        UUID publicId = UUID.randomUUID();
        SaveTripRequest request = createValidRequest();
        when(tripRepository.findByPublicIdAndUserId(publicId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.replaceTrip(userId, publicId, request))
                .isInstanceOf(TripNotFoundException.class);

        verifyNoInteractions(tripPolicy, tripMapper);
        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    void deletesOwnedTrip() {
        Long userId = 99L;
        Trip trip = createTrip(userId, OffsetDateTime.parse("2026-08-01T10:00:00+07:00"));
        UUID publicId = trip.getPublicId();
        when(tripRepository.findByPublicIdAndUserId(publicId, userId)).thenReturn(Optional.of(trip));

        tripService.deleteTrip(userId, publicId);

        verify(tripRepository).findByPublicIdAndUserId(publicId, userId);
        verify(tripRepository).delete(trip);
        verifyNoInteractions(tripPolicy, tripMapper);
    }

    @Test
    void throwsTripNotFoundBeforeDeletingAnotherUsersTrip() {
        Long requestingUserId = 99L;
        UUID publicId = UUID.randomUUID();
        when(tripRepository.findByPublicIdAndUserId(publicId, requestingUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.deleteTrip(requestingUserId, publicId))
                .isInstanceOf(TripNotFoundException.class);

        verify(tripRepository, never()).delete(any(Trip.class));
        verifyNoInteractions(tripPolicy, tripMapper);
    }

    @Test
    void throwsTripNotFoundBeforeDeletingNonexistentTrip() {
        Long userId = 99L;
        UUID publicId = UUID.randomUUID();
        when(tripRepository.findByPublicIdAndUserId(publicId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.deleteTrip(userId, publicId)).isInstanceOf(TripNotFoundException.class);

        verify(tripRepository, never()).delete(any(Trip.class));
    }

    private Trip createTrip(Long userId, OffsetDateTime timestamp) {
        return new Trip(
                userId,
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
    }

    private SaveTripRequest createValidRequest() {
        return new SaveTripRequest(
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                new StartLocationRequest("Chợ Bến Thành", new BigDecimal("10.7726400"), new BigDecimal("106.6980500")),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED);
    }

    private TripResponse responseFor(
            UUID publicId, SaveTripRequest request, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new TripResponse(
                publicId,
                request.tripDate(),
                request.startTime(),
                request.endTime(),
                request.budget(),
                new StartLocationResponse(
                        request.startLocation().label(),
                        request.startLocation().latitude(),
                        request.startLocation().longitude()),
                request.travelPace(),
                request.environmentPreference(),
                createdAt,
                updatedAt);
    }

    private TripSummaryResponse summaryFor(Trip trip) {
        return new TripSummaryResponse(
                trip.getPublicId(),
                trip.getTripDate(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getBudget(),
                trip.getStartLocationLabel(),
                trip.getTravelPace(),
                trip.getEnvironmentPreference(),
                trip.getUpdatedAt());
    }
}
