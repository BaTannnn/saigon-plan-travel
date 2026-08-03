package com.saigonplantravel.backend.trip.service;

import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.service.CategoryService;
import com.saigonplantravel.backend.trip.domain.EnvironmentPreference;
import com.saigonplantravel.backend.trip.domain.TravelPace;
import com.saigonplantravel.backend.trip.domain.TripPolicy;
import com.saigonplantravel.backend.trip.dto.SaveTripRequest;
import com.saigonplantravel.backend.trip.dto.StartLocationRequest;
import com.saigonplantravel.backend.trip.dto.StartLocationResponse;
import com.saigonplantravel.backend.trip.dto.TripResponse;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.InvalidCategoryPreferenceException;
import com.saigonplantravel.backend.trip.mapper.TripMapper;
import com.saigonplantravel.backend.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

import com.saigonplantravel.backend.trip.exception.TripNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private TripPolicy tripPolicy;

    @Mock
    private TripMapper tripMapper;

    private TripService tripService;

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
                Instant.parse("2026-08-01T03:00:00Z"),
                ZoneId.of("Asia/Ho_Chi_Minh")
        );

        tripService = new TripService(
                tripRepository,
                categoryService,
                tripPolicy,
                tripMapper,
                fixedClock
        );
    }

    @Test
    void createsDraftWithResolvedCategoryIds() {
        SaveTripRequest request =
                createValidRequest();

        List<CategoryResponse> categories = List.of(
                new CategoryResponse(
                        2L,
                        "Nghệ thuật",
                        "nghe-thuat"
                ),
                new CategoryResponse(
                        1L,
                        "Văn hóa",
                        "van-hoa"
                )
        );

        OffsetDateTime now = OffsetDateTime.parse(
                "2026-08-01T10:00:00+07:00"
        );

        TripResponse expectedResponse =
                new TripResponse(
                        UUID.randomUUID(),
                        request.tripDate(),
                        request.startTime(),
                        request.endTime(),
                        request.budget(),
                        new StartLocationResponse(
                                request.startLocation().label(),
                                request.startLocation().latitude(),
                                request.startLocation().longitude()
                        ),
                        request.travelPace(),
                        request.environmentPreference(),
                        categories,
                        now,
                        now
                );

        when(
                categoryService.findCategoriesBySlugs(
                        request.categorySlugs()
                )
        ).thenReturn(categories);

        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        when(
                tripMapper.toResponse(
                        any(Trip.class),
                        same(categories)
                )
        ).thenReturn(expectedResponse);

        TripResponse response =
                tripService.createTrip(
                        99L,
                        request
                );

        assertThat(response)
                .isSameAs(expectedResponse);

        verify(tripPolicy).validate(
                request.tripDate(),
                request.startTime(),
                request.endTime(),
                request.categorySlugs()
        );

        verify(categoryService)
                .findCategoriesBySlugs(
                        request.categorySlugs()
                );

        ArgumentCaptor<Trip> tripCaptor =
                ArgumentCaptor.forClass(Trip.class);

        verify(tripRepository)
                .save(tripCaptor.capture());

        Trip savedTrip = tripCaptor.getValue();

        assertThat(savedTrip.getUserId())
                .isEqualTo(99L);

        assertThat(savedTrip.getTripDate())
                .isEqualTo(request.tripDate());

        assertThat(savedTrip.getStartTime())
                .isEqualTo(request.startTime());

        assertThat(savedTrip.getEndTime())
                .isEqualTo(request.endTime());

        assertThat(savedTrip.getBudget())
                .isEqualByComparingTo(
                        request.budget()
                );

        assertThat(savedTrip.getStartLocationLabel())
                .isEqualTo("Chợ Bến Thành");

        assertThat(savedTrip.getPreferredCategoryIds())
                .containsExactlyInAnyOrder(
                        1L,
                        2L
                );

        assertThat(savedTrip.getCreatedAt())
                .isEqualTo(now);

        assertThat(savedTrip.getUpdatedAt())
                .isEqualTo(now);

        verify(tripMapper).toResponse(
                savedTrip,
                categories
        );
    }
    @Test
    void getsOwnedDraftWithResolvedCategories() {
        Long userId = 99L;
        UUID publicId = UUID.randomUUID();

        OffsetDateTime createdAt =
                OffsetDateTime.parse(
                        "2026-08-01T10:00:00+07:00"
                );

        Trip trip = new Trip(
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
                Set.of(1L, 2L),
                createdAt
        );

        List<CategoryResponse> categories = List.of(
                new CategoryResponse(
                        2L,
                        "Nghệ thuật",
                        "nghe-thuat"
                ),
                new CategoryResponse(
                        1L,
                        "Văn hóa",
                        "van-hoa"
                )
        );

        TripResponse expectedResponse =
                new TripResponse(
                        trip.getPublicId(),
                        trip.getTripDate(),
                        trip.getStartTime(),
                        trip.getEndTime(),
                        trip.getBudget(),
                        new StartLocationResponse(
                                trip.getStartLocationLabel(),
                                trip.getStartLatitude(),
                                trip.getStartLongitude()
                        ),
                        trip.getTravelPace(),
                        trip.getEnvironmentPreference(),
                        categories,
                        trip.getCreatedAt(),
                        trip.getUpdatedAt()
                );

        when(
                tripRepository.findByPublicIdAndUserId(
                        publicId,
                        userId
                )
        ).thenReturn(Optional.of(trip));

        when(
                categoryService.findCategoriesByIds(
                        trip.getPreferredCategoryIds()
                )
        ).thenReturn(categories);

        when(
                tripMapper.toResponse(
                        trip,
                        categories
                )
        ).thenReturn(expectedResponse);

        TripResponse response =
                tripService.getTrip(
                        userId,
                        publicId
                );

        assertThat(response)
                .isSameAs(expectedResponse);

        verify(tripRepository)
                .findByPublicIdAndUserId(
                        publicId,
                        userId
                );

        verify(categoryService)
                .findCategoriesByIds(
                        trip.getPreferredCategoryIds()
                );

        verify(tripMapper)
                .toResponse(
                        trip,
                        categories
                );
    }
    @Test
    void rejectsUnknownCategoryBeforeSavingTrip() {
        SaveTripRequest request =
                createValidRequest();

        when(
                categoryService.findCategoriesBySlugs(
                        request.categorySlugs()
                )
        ).thenReturn(
                List.of(
                        new CategoryResponse(
                                1L,
                                "Văn hóa",
                                "van-hoa"
                        )
                )
        );

        assertThatThrownBy(
                () -> tripService.createTrip(
                        99L,
                        request
                )
        )
                .isInstanceOf(
                        InvalidCategoryPreferenceException.class
                )
                .satisfies(exception -> {
                    InvalidCategoryPreferenceException
                            categoryException =
                            (InvalidCategoryPreferenceException)
                                    exception;

                    assertThat(
                            categoryException
                                    .getUnknownCategorySlugs()
                    ).containsExactly("nghe-thuat");
                });

        verify(tripPolicy).validate(
                request.tripDate(),
                request.startTime(),
                request.endTime(),
                request.categorySlugs()
        );

        verify(tripRepository, never())
                .save(any(Trip.class));

        verify(tripMapper, never())
                .toResponse(
                        any(Trip.class),
                        anyList()
                );
    }
    @Test
    void throwsTripNotFoundWhenDraftDoesNotBelongToUser() {
        Long userId = 99L;
        UUID publicId = UUID.randomUUID();

        when(
                tripRepository.findByPublicIdAndUserId(
                        publicId,
                        userId
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> tripService.getTrip(
                        userId,
                        publicId
                )
        )
                .isInstanceOf(
                        TripNotFoundException.class
                )
                .hasMessage("Trip not found");

        verify(tripRepository)
                .findByPublicIdAndUserId(
                        publicId,
                        userId
                );

        verify(categoryService, never())
                .findCategoriesByIds(any());

        verify(tripMapper, never())
                .toResponse(
                        any(Trip.class),
                        anyList()
                );
    }
    @Test
    void replacesOwnedTripWithoutChangingIdentityOrCreatedAt() {
        Long userId = 99L;

        OffsetDateTime createdAt =
                OffsetDateTime.parse(
                        "2026-07-31T09:00:00+07:00"
                );

        Trip trip = new Trip(
                userId,
                LocalDate.of(2026, 8, 10),
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                new BigDecimal("300000.00"),
                "Địa điểm xuất phát cũ",
                new BigDecimal("10.7700000"),
                new BigDecimal("106.6900000"),
                TravelPace.FAST,
                EnvironmentPreference.OUTDOOR,
                Set.of(1L),
                createdAt
        );

        UUID publicId = trip.getPublicId();

        SaveTripRequest request =
                new SaveTripRequest(
                        LocalDate.of(2026, 8, 20),
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        new BigDecimal("700000.00"),
                        new StartLocationRequest(
                                "Bưu điện Trung tâm Sài Gòn",
                                new BigDecimal("10.7798000"),
                                new BigDecimal("106.6990000")
                        ),
                        TravelPace.RELAXED,
                        EnvironmentPreference.INDOOR,
                        List.of(
                                "am-thuc",
                                "nghe-thuat"
                        )
                );

        List<CategoryResponse> categories = List.of(
                new CategoryResponse(
                        4L,
                        "Ẩm thực",
                        "am-thuc"
                ),
                new CategoryResponse(
                        2L,
                        "Nghệ thuật",
                        "nghe-thuat"
                )
        );

        OffsetDateTime updatedAt =
                OffsetDateTime.parse(
                        "2026-08-01T10:00:00+07:00"
                );

        TripResponse expectedResponse =
                new TripResponse(
                        publicId,
                        request.tripDate(),
                        request.startTime(),
                        request.endTime(),
                        request.budget(),
                        new StartLocationResponse(
                                request.startLocation().label(),
                                request.startLocation().latitude(),
                                request.startLocation().longitude()
                        ),
                        request.travelPace(),
                        request.environmentPreference(),
                        categories,
                        createdAt,
                        updatedAt
                );

        when(
                tripRepository.findByPublicIdAndUserId(
                        publicId,
                        userId
                )
        ).thenReturn(Optional.of(trip));

        when(
                categoryService.findCategoriesBySlugs(
                        request.categorySlugs()
                )
        ).thenReturn(categories);

        when(
                tripMapper.toResponse(
                        trip,
                        categories
                )
        ).thenReturn(expectedResponse);

        TripResponse response =
                tripService.replaceTrip(
                        userId,
                        publicId,
                        request
                );

        assertThat(response)
                .isSameAs(expectedResponse);

        assertThat(trip.getPublicId())
                .isEqualTo(publicId);

        assertThat(trip.getUserId())
                .isEqualTo(userId);

        assertThat(trip.getCreatedAt())
                .isEqualTo(createdAt);

        assertThat(trip.getUpdatedAt())
                .isEqualTo(updatedAt);

        assertThat(trip.getTripDate())
                .isEqualTo(request.tripDate());

        assertThat(trip.getStartTime())
                .isEqualTo(request.startTime());

        assertThat(trip.getEndTime())
                .isEqualTo(request.endTime());

        assertThat(trip.getBudget())
                .isEqualByComparingTo(
                        request.budget()
                );

        assertThat(trip.getStartLocationLabel())
                .isEqualTo(
                        "Bưu điện Trung tâm Sài Gòn"
                );

        assertThat(trip.getStartLatitude())
                .isEqualByComparingTo("10.7798000");

        assertThat(trip.getStartLongitude())
                .isEqualByComparingTo("106.6990000");

        assertThat(trip.getTravelPace())
                .isEqualTo(TravelPace.RELAXED);

        assertThat(trip.getEnvironmentPreference())
                .isEqualTo(
                        EnvironmentPreference.INDOOR
                );

        assertThat(trip.getPreferredCategoryIds())
                .containsExactlyInAnyOrder(
                        2L,
                        4L
                );

        verify(tripPolicy).validate(
                request.tripDate(),
                request.startTime(),
                request.endTime(),
                request.categorySlugs()
        );

        verify(tripRepository, never())
                .save(any(Trip.class));

        verify(tripMapper).toResponse(
                trip,
                categories
        );
    }
    @Test
    void throwsTripNotFoundBeforeReplacingUnownedTrip() {
        Long userId = 99L;
        UUID publicId = UUID.randomUUID();

        SaveTripRequest request =
                createValidRequest();

        when(
                tripRepository.findByPublicIdAndUserId(
                        publicId,
                        userId
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> tripService.replaceTrip(
                        userId,
                        publicId,
                        request
                )
        )
                .isInstanceOf(
                        TripNotFoundException.class
                );

        verifyNoInteractions(
                tripPolicy,
                categoryService,
                tripMapper
        );

        verify(tripRepository, never())
                .save(any(Trip.class));
    }
    @Test
    void doesNotMutateTripWhenCategoryIsUnknown() {
        Long userId = 99L;

        OffsetDateTime originalTimestamp =
                OffsetDateTime.parse(
                        "2026-07-31T09:00:00+07:00"
                );

        Trip trip = new Trip(
                userId,
                LocalDate.of(2026, 8, 10),
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                new BigDecimal("300000.00"),
                "Địa điểm xuất phát cũ",
                new BigDecimal("10.7700000"),
                new BigDecimal("106.6900000"),
                TravelPace.FAST,
                EnvironmentPreference.OUTDOOR,
                Set.of(1L),
                originalTimestamp
        );

        UUID publicId = trip.getPublicId();

        SaveTripRequest request =
                new SaveTripRequest(
                        LocalDate.of(2026, 8, 20),
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        new BigDecimal("700000.00"),
                        new StartLocationRequest(
                                "Địa điểm mới",
                                new BigDecimal("10.7798000"),
                                new BigDecimal("106.6990000")
                        ),
                        TravelPace.RELAXED,
                        EnvironmentPreference.INDOOR,
                        List.of(
                                "van-hoa",
                                "khong-ton-tai"
                        )
                );

        when(
                tripRepository.findByPublicIdAndUserId(
                        publicId,
                        userId
                )
        ).thenReturn(Optional.of(trip));

        when(
                categoryService.findCategoriesBySlugs(
                        request.categorySlugs()
                )
        ).thenReturn(
                List.of(
                        new CategoryResponse(
                                1L,
                                "Văn hóa",
                                "van-hoa"
                        )
                )
        );

        assertThatThrownBy(
                () -> tripService.replaceTrip(
                        userId,
                        publicId,
                        request
                )
        )
                .isInstanceOf(
                        InvalidCategoryPreferenceException.class
                );

        assertThat(trip.getTripDate())
                .isEqualTo(
                        LocalDate.of(2026, 8, 10)
                );

        assertThat(trip.getStartTime())
                .isEqualTo(
                        LocalTime.of(8, 0)
                );

        assertThat(trip.getEndTime())
                .isEqualTo(
                        LocalTime.of(16, 0)
                );

        assertThat(trip.getBudget())
                .isEqualByComparingTo("300000.00");

        assertThat(trip.getStartLocationLabel())
                .isEqualTo(
                        "Địa điểm xuất phát cũ"
                );

        assertThat(trip.getTravelPace())
                .isEqualTo(TravelPace.FAST);

        assertThat(trip.getEnvironmentPreference())
                .isEqualTo(
                        EnvironmentPreference.OUTDOOR
                );

        assertThat(trip.getPreferredCategoryIds())
                .containsExactly(1L);

        assertThat(trip.getCreatedAt())
                .isEqualTo(originalTimestamp);

        assertThat(trip.getUpdatedAt())
                .isEqualTo(originalTimestamp);

        verify(tripRepository, never())
                .save(any(Trip.class));

        verify(tripMapper, never())
                .toResponse(
                        any(Trip.class),
                        anyList()
                );
    }
    @Test
    void returnsOwnedTripSchedulingSnapshot() {
        Long userId = 99L;

        UUID publicId = UUID.fromString(
                "7a674ef0-57c8-4d0e-b99b-dccfd342fc98"
        );

        OffsetDateTime updatedAt =
                OffsetDateTime.parse(
                        "2026-08-03T10:00:00+07:00"
                );

        Set<Long> categoryIds =
                new HashSet<>(
                        Set.of(1L, 2L)
                );

        Trip trip = mock(Trip.class);

        when(trip.getId())
                .thenReturn(321L);

        when(trip.getPublicId())
                .thenReturn(publicId);

        when(trip.getTripDate())
                .thenReturn(
                        LocalDate.of(2026, 8, 20)
                );

        when(trip.getStartTime())
                .thenReturn(
                        LocalTime.of(8, 0)
                );

        when(trip.getEndTime())
                .thenReturn(
                        LocalTime.of(18, 0)
                );

        when(trip.getBudget())
                .thenReturn(
                        new BigDecimal("500000.00")
                );

        when(trip.getStartLatitude())
                .thenReturn(
                        new BigDecimal("10.7726400")
                );

        when(trip.getStartLongitude())
                .thenReturn(
                        new BigDecimal("106.6980500")
                );

        when(trip.getTravelPace())
                .thenReturn(TravelPace.BALANCED);

        when(trip.getEnvironmentPreference())
                .thenReturn(
                        EnvironmentPreference.MIXED
                );

        when(trip.getPreferredCategoryIds())
                .thenReturn(categoryIds);

        when(trip.getUpdatedAt())
                .thenReturn(updatedAt);

        when(
                tripRepository.findByPublicIdAndUserId(
                        publicId,
                        userId
                )
        ).thenReturn(Optional.of(trip));

        TripSchedulingSnapshot snapshot =
                tripService.getByPublicId(
                        publicId,
                        userId
                );

        assertThat(snapshot.tripId())
                .isEqualTo(321L);

        assertThat(snapshot.publicId())
                .isEqualTo(publicId);

        assertThat(snapshot.tripDate())
                .isEqualTo(
                        LocalDate.of(2026, 8, 20)
                );

        assertThat(snapshot.startTime())
                .isEqualTo(
                        LocalTime.of(8, 0)
                );

        assertThat(snapshot.endTime())
                .isEqualTo(
                        LocalTime.of(18, 0)
                );

        assertThat(snapshot.budget())
                .isEqualByComparingTo("500000.00");

        assertThat(snapshot.startLatitude())
                .isEqualByComparingTo("10.7726400");

        assertThat(snapshot.startLongitude())
                .isEqualByComparingTo("106.6980500");

        assertThat(snapshot.travelPace())
                .isEqualTo(TravelPace.BALANCED);

        assertThat(snapshot.environmentPreference())
                .isEqualTo(
                        EnvironmentPreference.MIXED
                );

        assertThat(snapshot.preferredCategoryIds())
                .containsExactlyInAnyOrder(
                        1L,
                        2L
                );

        assertThat(snapshot.updatedAt())
                .isEqualTo(updatedAt);

        verify(tripRepository)
                .findByPublicIdAndUserId(
                        publicId,
                        userId
                );

        verifyNoInteractions(
                categoryService,
                tripPolicy,
                tripMapper
        );
        categoryIds.add(3L);

        assertThat(snapshot.preferredCategoryIds())
                .containsExactlyInAnyOrder(
                        1L,
                        2L
                );

        assertThatThrownBy(
                () -> snapshot
                        .preferredCategoryIds()
                        .add(4L)
        )
                .isInstanceOf(
                        UnsupportedOperationException.class
                );
    }
    @Test
    void throwsTripNotFoundWhenSchedulingTripIsMissingOrUnowned() {
        Long userId = 99L;
        UUID publicId = UUID.randomUUID();

        when(
                tripRepository.findByPublicIdAndUserId(
                        publicId,
                        userId
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> tripService.getByPublicId(
                        publicId,
                        userId
                )
        )
                .isInstanceOf(
                        TripNotFoundException.class
                )
                .hasMessage("Trip not found");

        verify(tripRepository)
                .findByPublicIdAndUserId(
                        publicId,
                        userId
                );

        verifyNoInteractions(
                categoryService,
                tripPolicy,
                tripMapper
        );
    }
    private SaveTripRequest createValidRequest() {
        return new SaveTripRequest(
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                new BigDecimal("500000.00"),
                new StartLocationRequest(
                        "Chợ Bến Thành",
                        new BigDecimal("10.7726400"),
                        new BigDecimal("106.6980500")
                ),
                TravelPace.BALANCED,
                EnvironmentPreference.MIXED,
                List.of(
                        "van-hoa",
                        "nghe-thuat"
                )
        );
    }
}