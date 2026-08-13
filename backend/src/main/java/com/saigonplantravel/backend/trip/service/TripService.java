package com.saigonplantravel.backend.trip.service;

import com.saigonplantravel.backend.trip.domain.TripPolicy;
import com.saigonplantravel.backend.trip.dto.SaveTripRequest;
import com.saigonplantravel.backend.trip.dto.TripResponse;
import com.saigonplantravel.backend.trip.dto.TripSummaryResponse;
import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import com.saigonplantravel.backend.trip.mapper.TripMapper;
import com.saigonplantravel.backend.trip.repository.TripRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TripPolicy tripPolicy;
    private final TripMapper tripMapper;
    private final Clock clock;

    public TripService(
            TripRepository tripRepository,
            TripPolicy tripPolicy,
            TripMapper tripMapper,
            Clock clock) {
        this.tripRepository = tripRepository;
        this.tripPolicy = tripPolicy;
        this.tripMapper = tripMapper;
        this.clock = clock;
    }

    @Transactional
    public TripResponse createTrip(Long userId, SaveTripRequest request) {
        tripPolicy.validate(request.tripDate(), request.startTime(), request.endTime());

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
                now);

        Trip savedTrip = tripRepository.save(trip);

        return tripMapper.toResponse(savedTrip);
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(Long userId, UUID publicId) {
        Trip trip = tripRepository.findByPublicIdAndUserId(publicId, userId).orElseThrow(TripNotFoundException::new);

        return tripMapper.toResponse(trip);
    }

    @Transactional(readOnly = true)
    public List<TripSummaryResponse> listTrips(Long userId) {
        List<Trip> trips = tripRepository.findAllByUserIdOrderByTripDateAscStartTimeAscPublicIdAsc(userId);

        return trips.stream().map(tripMapper::toSummaryResponse).toList();
    }

    @Transactional
    public TripResponse replaceTrip(Long userId, UUID publicId, SaveTripRequest request) {
        Trip trip = tripRepository.findByPublicIdAndUserId(publicId, userId).orElseThrow(TripNotFoundException::new);

        tripPolicy.validate(request.tripDate(), request.startTime(), request.endTime());

        OffsetDateTime now = OffsetDateTime.now(clock);

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
                now);

        return tripMapper.toResponse(trip);
    }
}
