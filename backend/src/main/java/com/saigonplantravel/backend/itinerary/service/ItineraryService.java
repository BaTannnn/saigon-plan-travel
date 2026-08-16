package com.saigonplantravel.backend.itinerary.service;

import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailResponse;
import com.saigonplantravel.backend.itinerary.entity.Itinerary;
import com.saigonplantravel.backend.itinerary.entity.ItineraryItem;
import com.saigonplantravel.backend.itinerary.exception.DuplicateItineraryPlaceException;
import com.saigonplantravel.backend.itinerary.exception.InactiveItineraryPlaceException;
import com.saigonplantravel.backend.itinerary.exception.InvalidItineraryOrderException;
import com.saigonplantravel.backend.itinerary.exception.ItineraryItemNotFoundException;
import com.saigonplantravel.backend.itinerary.mapper.ItineraryDetailMapper;
import com.saigonplantravel.backend.itinerary.model.CalculatedItinerary;
import com.saigonplantravel.backend.itinerary.repository.ItineraryRepository;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.exception.PlaceNotFoundException;
import com.saigonplantravel.backend.place.repository.PlaceRepository;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import com.saigonplantravel.backend.trip.repository.TripRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItineraryService {

    private final TripRepository tripRepository;
    private final ItineraryRepository itineraryRepository;
    private final PlaceRepository placeRepository;
    private final ItineraryRecalculationService recalculationService;
    private final ItineraryDetailMapper itineraryDetailMapper;
    private final Clock clock;

    public ItineraryService(
            TripRepository tripRepository,
            ItineraryRepository itineraryRepository,
            PlaceRepository placeRepository,
            ItineraryRecalculationService recalculationService,
            ItineraryDetailMapper itineraryDetailMapper,
            Clock clock) {

        this.tripRepository = tripRepository;
        this.itineraryRepository = itineraryRepository;
        this.placeRepository = placeRepository;
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
        Itinerary itinerary = findItineraryWithItem(trip.getId(), itemPublicId);
        ItineraryItem item = itinerary.findItem(itemPublicId).orElseThrow(ItineraryItemNotFoundException::new);
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
        Itinerary itinerary = findItineraryWithItem(trip.getId(), itemPublicId);
        ItineraryItem item = itinerary.findItem(itemPublicId).orElseThrow(ItineraryItemNotFoundException::new);
        Place replacementPlace = findActivePlace(replacementPlaceId);

        if (itinerary.containsOtherPlace(replacementPlaceId, itemPublicId)) {
            throw new DuplicateItineraryPlaceException();
        }

        itinerary.replaceItemPlace(item, replacementPlace, OffsetDateTime.now(clock));

        return recalculateAndMap(tripPublicId, trip, itinerary);
    }

    private Trip findOwnedTrip(Long userId, UUID tripPublicId) {
        return tripRepository.findByPublicIdAndUserId(tripPublicId, userId).orElseThrow(TripNotFoundException::new);
    }

    private Itinerary findItineraryWithItem(Long tripId, UUID itemPublicId) {
        Itinerary itinerary = itineraryRepository.findByTripId(tripId).orElseThrow(ItineraryItemNotFoundException::new);

        if (itinerary.findItem(itemPublicId).isEmpty()) {
            throw new ItineraryItemNotFoundException();
        }

        return itinerary;
    }

    private Place findActivePlace(Long placeId) {
        Place place = placeRepository.findById(placeId).orElseThrow(PlaceNotFoundException::new);

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
}
