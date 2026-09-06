package com.saigonplantravel.backend.itinerary.service;

import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailResponse;
import com.saigonplantravel.backend.itinerary.entity.Itinerary;
import com.saigonplantravel.backend.itinerary.entity.ItineraryItem;
import com.saigonplantravel.backend.itinerary.exception.*;
import com.saigonplantravel.backend.itinerary.mapper.ItineraryDetailMapper;
import com.saigonplantravel.backend.itinerary.model.CalculatedItinerary;
import com.saigonplantravel.backend.itinerary.repository.ItineraryRepository;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.service.PlaceQueryService;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.service.TripQueryService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItineraryService {

    private final TripQueryService tripQueryService;
    private final ItineraryRepository itineraryRepository;
    private final PlaceQueryService placeQueryService;
    private final ItineraryRecalculationService recalculationService;
    private final ItineraryDetailMapper itineraryDetailMapper;
    private final Clock clock;

    public ItineraryService(
            TripQueryService tripQueryService,
            ItineraryRepository itineraryRepository,
            PlaceQueryService placeQueryService,
            ItineraryRecalculationService recalculationService,
            ItineraryDetailMapper itineraryDetailMapper,
            Clock clock) {

        this.tripQueryService = tripQueryService;
        this.itineraryRepository = itineraryRepository;
        this.placeQueryService = placeQueryService;
        this.recalculationService = recalculationService;
        this.itineraryDetailMapper = itineraryDetailMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ItineraryDetailResponse getItinerary(Long userId, UUID tripPublicId) {

        Trip trip = findOwnedTrip(userId, tripPublicId);

        return itineraryRepository
                .findByTripId(trip.getId())
                .map(itinerary -> recalculateAndMap(tripPublicId, trip, itinerary))
                .orElseGet(() -> calculateEmptyItinerary(tripPublicId, trip));
    }

    @Transactional
    public ItineraryDetailResponse addItem(Long userId, UUID tripPublicId, Long placeId) {

        Trip trip = findOwnedTrip(userId, tripPublicId);

        Place place = findActivePlace(placeId);

        OffsetDateTime now = OffsetDateTime.now(clock);

        Itinerary itinerary = itineraryRepository.findByTripId(trip.getId()).orElseGet(() -> new Itinerary(trip, now));

        if (itinerary.containsPlace(placeId)) {

            throw new DuplicateItineraryPlaceException();
        }

        itinerary.appendItem(place, now);

        Itinerary saved = itineraryRepository.save(itinerary);

        return recalculateAndMap(tripPublicId, trip, saved);
    }

    @Transactional
    public ItineraryDetailResponse deleteItem(Long userId, UUID tripPublicId, UUID itemPublicId) {
        Trip trip = findOwnedTrip(userId, tripPublicId);
        Itinerary itinerary = findItinerary(trip.getId());
        ItineraryItem item = findItem(itinerary, itemPublicId);
        OffsetDateTime now = OffsetDateTime.now(clock);

        itinerary.removeItem(item, now);

        itineraryRepository.flush();

        itinerary.resequenceItems(now);

        return recalculateAndMap(tripPublicId, trip, itinerary);
    }

    @Transactional
    public ItineraryDetailResponse replaceItemPlace(
            Long userId, UUID tripPublicId, UUID itemPublicId, Long replacementPlaceId) {
        Trip trip = findOwnedTrip(userId, tripPublicId);
        Itinerary itinerary = findItinerary(trip.getId());
        ItineraryItem item = findItem(itinerary, itemPublicId);
        Place replacementPlace = findActivePlace(replacementPlaceId);

        if (itinerary.containsOtherPlace(replacementPlaceId, itemPublicId)) {
            throw new DuplicateItineraryPlaceException();
        }

        itinerary.replaceItemPlace(item, replacementPlace, OffsetDateTime.now(clock));

        return recalculateAndMap(tripPublicId, trip, itinerary);
    }

    private Trip findOwnedTrip(Long userId, UUID tripPublicId) {
        return tripQueryService.findOwnedTrip(userId, tripPublicId);
    }

    private Itinerary findItinerary(Long tripId) {
        return itineraryRepository.findByTripId(tripId)
            .orElseThrow(ItineraryItemNotFoundException::new);
    }

    private ItineraryItem findItem(Itinerary itinerary, UUID itemPublicId) {
        return itinerary.findItem(itemPublicId)
                .orElseThrow(ItineraryItemNotFoundException::new);
    }

    private Place findActivePlace(Long placeId) {
        Place place = placeQueryService.findPlace(placeId);

        if (!Boolean.TRUE.equals(place.getActive())) {
            throw new InactiveItineraryPlaceException();
        }

        return place;
    }

    private ItineraryDetailResponse recalculateAndMap(UUID tripPublicId, Trip trip, Itinerary itinerary) {

        List<Place> orderedPlaces =
                itinerary.getItems().stream().map(ItineraryItem::getPlace).toList();

        CalculatedItinerary calculatedItinerary = recalculationService.recalculate(trip, orderedPlaces);

        return itineraryDetailMapper.toResponse(tripPublicId, itinerary, calculatedItinerary);
    }

    private ItineraryDetailResponse calculateEmptyItinerary(UUID tripPublicId, Trip trip) {

        CalculatedItinerary calculated = recalculationService.recalculate(trip, List.of());

        return itineraryDetailMapper.toEmptyResponse(tripPublicId, calculated);
    }

    @Transactional
    public ItineraryDetailResponse reorderItems(Long userId, UUID tripPublicId, List<UUID> orderedItemPublicIds) {

        Trip trip = findOwnedTrip(userId, tripPublicId);

        Itinerary itinerary =
                itineraryRepository.findByTripId(trip.getId()).orElseThrow(InvalidItineraryOrderException::new);

        OffsetDateTime now = OffsetDateTime.now(clock);

        /*
         * Phase 1:
         * Move current sequence numbers outside
         * the active range to avoid violating
         * UNIQUE(itinerary_id, sequence_no).
         */
        itinerary.shiftSequencesForReorder(now);

        itineraryRepository.flush();

        /*
         * Phase 2:
         * Apply the user-defined final order.
         */
        itinerary.reorderItems(orderedItemPublicIds, now);

        return recalculateAndMap(tripPublicId, trip, itinerary);
    }

    @Transactional
    public ItineraryDetailResponse applyGeneratedItinerary(Long userId, UUID tripPublicId, List<String> placeSlugs) {

        Trip trip = findOwnedTrip(userId, tripPublicId);

        List<Place> orderedPlaces = resolveGeneratedPlaces(placeSlugs);

        OffsetDateTime now = OffsetDateTime.now(clock);

        Itinerary itinerary = itineraryRepository.findByTripId(trip.getId()).orElse(null);

        if (itinerary == null) {

            itinerary = new Itinerary(trip, now);

        } else {

            /*
             * User đã xác nhận replace preview AI.
             *
             * Xóa itinerary items cũ trước,
             * rồi flush để giải phóng sequence_no.
             */
            itinerary.clearItems(now);

            itineraryRepository.flush();
        }

        for (Place place : orderedPlaces) {

            itinerary.appendItem(place, now);
        }

        Itinerary saved = itineraryRepository.save(itinerary);

        return recalculateAndMap(tripPublicId, trip, saved);
    }

    private List<Place> resolveGeneratedPlaces(List<String> placeSlugs) {

        if (placeSlugs == null || placeSlugs.isEmpty() || new HashSet<>(placeSlugs).size() != placeSlugs.size()) {

            throw new InvalidGeneratedItineraryException();
        }

        List<Place> places = placeQueryService.findAllActiveBySlugsForScheduling(placeSlugs);

        if (places.size() != placeSlugs.size()) {

            throw new InvalidGeneratedItineraryException();
        }

        Map<String, Place> placesBySlug = places.stream().collect(Collectors.toMap(Place::getSlug, place -> place));

        return placeSlugs.stream().map(placesBySlug::get).toList();
    }
}
