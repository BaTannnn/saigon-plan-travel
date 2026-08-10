package com.saigonplantravel.backend.trip.service;

import com.saigonplantravel.backend.place.dto.CategoryResponse;
import com.saigonplantravel.backend.place.service.CategoryService;
import com.saigonplantravel.backend.trip.domain.TripPolicy;
import com.saigonplantravel.backend.trip.dto.SaveTripRequest;
import com.saigonplantravel.backend.trip.dto.TripResponse;
import com.saigonplantravel.backend.trip.dto.TripSummaryResponse;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.InvalidCategoryPreferenceException;
import com.saigonplantravel.backend.trip.mapper.TripMapper;
import com.saigonplantravel.backend.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import java.util.UUID;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final CategoryService categoryService;
    private final TripPolicy tripPolicy;
    private final TripMapper tripMapper;
    private final Clock clock;

    public TripService(
            TripRepository tripRepository,
            CategoryService categoryService,
            TripPolicy tripPolicy,
            TripMapper tripMapper,
            Clock clock
    ) {
        this.tripRepository = tripRepository;
        this.categoryService = categoryService;
        this.tripPolicy = tripPolicy;
        this.tripMapper = tripMapper;
        this.clock = clock;
    }

    @Transactional
    public TripResponse createTrip(
            Long userId,
            SaveTripRequest request
    ) {
        tripPolicy.validate(
                request.tripDate(),
                request.startTime(),
                request.endTime(),
                request.categorySlugs()
        );

        List<CategoryResponse> categories =
                resolveCategories(request.categorySlugs());

        Set<Long> categoryIds = categories.stream()
                .map(CategoryResponse::id)
                .collect(Collectors.toUnmodifiableSet());

        OffsetDateTime now = OffsetDateTime.now(clock);

        Trip trip = new Trip(
                userId,
                request.tripDate(),
                request.startTime(),
                request.endTime(),
                request.budget(),
                request.startLocation().label(),
                request.startLocation().latitude(),
                request.startLocation().longitude(),
                request.travelPace(),
                request.environmentPreference(),
                categoryIds,
                now
        );

        Trip savedTrip = tripRepository.save(trip);

        return tripMapper.toResponse(
                savedTrip,
                categories
        );
    }
    @Transactional(readOnly = true)
    public TripResponse getTrip(
            Long userId,
            UUID publicId
    ) {
        Trip trip = tripRepository
                .findByPublicIdAndUserId(
                        publicId,
                        userId
                )
                .orElseThrow(
                        TripNotFoundException::new
                );

        List<CategoryResponse> categories =
                categoryService.findCategoriesByIds(
                        trip.getPreferredCategoryIds()
                );

        return tripMapper.toResponse(
                trip,
                categories
        );
    }

    @Transactional(readOnly = true)
    public List<TripSummaryResponse> listTrips(Long userId) {
        List<Trip> trips = tripRepository
                .findAllByUserIdOrderByTripDateAscStartTimeAscPublicIdAsc(
                        userId
                );

        if (trips.isEmpty()) {
            return List.of();
        }

        Set<Long> categoryIds = trips.stream()
                .flatMap(trip -> trip
                        .getPreferredCategoryIds()
                        .stream())
                .collect(Collectors.toSet());

        List<CategoryResponse> categories =
                categoryService.findCategoriesByIds(
                        categoryIds
                );

        return trips.stream()
                .map(trip -> tripMapper.toSummaryResponse(
                        trip,
                        categories.stream()
                                .filter(category -> trip
                                        .getPreferredCategoryIds()
                                        .contains(category.id()))
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public TripResponse replaceTrip(
            Long userId,
            UUID publicId,
            SaveTripRequest request
    ) {
        Trip trip = tripRepository
                .findByPublicIdAndUserId(
                        publicId,
                        userId
                )
                .orElseThrow(
                        TripNotFoundException::new
                );

        tripPolicy.validate(
                request.tripDate(),
                request.startTime(),
                request.endTime(),
                request.categorySlugs()
        );

        List<CategoryResponse> categories =
                resolveCategories(
                        request.categorySlugs()
                );

        Set<Long> categoryIds = categories.stream()
                .map(CategoryResponse::id)
                .collect(
                        Collectors.toUnmodifiableSet()
                );

        OffsetDateTime now =
                OffsetDateTime.now(clock);

        trip.replaceDetails(
                request.tripDate(),
                request.startTime(),
                request.endTime(),
                request.budget(),
                request.startLocation().label(),
                request.startLocation().latitude(),
                request.startLocation().longitude(),
                request.travelPace(),
                request.environmentPreference(),
                categoryIds,
                now
        );

        return tripMapper.toResponse(
                trip,
                categories
        );
    }
    private List<CategoryResponse> resolveCategories(
            List<String> requestedSlugs
    ) {
        List<CategoryResponse> categories =
                categoryService.findCategoriesBySlugs(
                        requestedSlugs
                );

        Set<String> resolvedSlugs = categories.stream()
                .map(CategoryResponse::slug)
                .collect(Collectors.toSet());

        List<String> unknownSlugs = requestedSlugs.stream()
                .filter(slug -> !resolvedSlugs.contains(slug))
                .distinct()
                .sorted()
                .toList();

        if (!unknownSlugs.isEmpty()) {
            throw new InvalidCategoryPreferenceException(
                    unknownSlugs
            );
        }

        return categories;
    }

}
