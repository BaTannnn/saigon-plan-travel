package com.saigonplantravel.backend.itinerary.service;

import com.saigonplantravel.backend.itinerary.dto.ItineraryResponse;
import com.saigonplantravel.backend.itinerary.entity.Itinerary;
import com.saigonplantravel.backend.itinerary.entity.ItineraryItem;
import com.saigonplantravel.backend.itinerary.exception.DuplicateItineraryPlaceException;
import com.saigonplantravel.backend.itinerary.exception.InactiveItineraryPlaceException;
import com.saigonplantravel.backend.itinerary.exception.ItineraryItemNotFoundException;
import com.saigonplantravel.backend.itinerary.mapper.ItineraryMapper;
import com.saigonplantravel.backend.itinerary.repository.ItineraryRepository;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.exception.PlaceNotFoundException;
import com.saigonplantravel.backend.place.repository.PlaceRepository;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import com.saigonplantravel.backend.trip.repository.TripRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItineraryService {

    private final TripRepository tripRepository;
    private final ItineraryRepository itineraryRepository;
    private final PlaceRepository placeRepository;
    private final ItineraryMapper itineraryMapper;
    private final Clock clock;

    public ItineraryService(
            TripRepository tripRepository,
            ItineraryRepository itineraryRepository,
            PlaceRepository placeRepository,
            ItineraryMapper itineraryMapper,
            Clock clock) {
        this.tripRepository = tripRepository;
        this.itineraryRepository = itineraryRepository;
        this.placeRepository = placeRepository;
        this.itineraryMapper = itineraryMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ItineraryResponse getItinerary(Long userId, UUID tripPublicId) {
        Trip trip = findOwnedTrip(userId, tripPublicId);

        return itineraryRepository
                .findByTripId(trip.getId())
                .map(itinerary -> itineraryMapper.toResponse(tripPublicId, itinerary))
                .orElseGet(() -> itineraryMapper.toEmptyResponse(tripPublicId));
    }

    @Transactional
    public ItineraryResponse addItem(Long userId, UUID tripPublicId, Long placeId) {
        Trip trip = findOwnedTrip(userId, tripPublicId);
        Place place = findActivePlace(placeId);
        OffsetDateTime now = OffsetDateTime.now(clock);

        Itinerary itinerary = itineraryRepository.findByTripId(trip.getId()).orElseGet(() -> new Itinerary(trip, now));

        if (itinerary.containsPlace(placeId)) {
            throw new DuplicateItineraryPlaceException();
        }

        itinerary.appendItem(place, now);
        Itinerary saved = itineraryRepository.save(itinerary);

        return itineraryMapper.toResponse(tripPublicId, saved);
    }

    @Transactional
    public ItineraryResponse deleteItem(Long userId, UUID tripPublicId, UUID itemPublicId) {
        Trip trip = findOwnedTrip(userId, tripPublicId);
        Itinerary itinerary = findItineraryWithItem(trip.getId(), itemPublicId);
        ItineraryItem item = itinerary.findItem(itemPublicId).orElseThrow(ItineraryItemNotFoundException::new);
        OffsetDateTime now = OffsetDateTime.now(clock);

        itinerary.removeItem(item, now);

        // The removed sequence must be deleted before lower sequence values
        // are written, otherwise the database unique constraint can collide.
        itineraryRepository.flush();

        itinerary.resequenceItems(now);

        return itineraryMapper.toResponse(tripPublicId, itinerary);
    }

    @Transactional
    public ItineraryResponse replaceItemPlace(
            Long userId, UUID tripPublicId, UUID itemPublicId, Long replacementPlaceId) {
        Trip trip = findOwnedTrip(userId, tripPublicId);
        Itinerary itinerary = findItineraryWithItem(trip.getId(), itemPublicId);
        ItineraryItem item = itinerary.findItem(itemPublicId).orElseThrow(ItineraryItemNotFoundException::new);
        Place replacementPlace = findActivePlace(replacementPlaceId);

        if (itinerary.containsOtherPlace(replacementPlaceId, itemPublicId)) {
            throw new DuplicateItineraryPlaceException();
        }

        itinerary.replaceItemPlace(item, replacementPlace, OffsetDateTime.now(clock));

        return itineraryMapper.toResponse(tripPublicId, itinerary);
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
}
