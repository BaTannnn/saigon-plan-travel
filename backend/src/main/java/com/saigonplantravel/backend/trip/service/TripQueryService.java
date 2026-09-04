package com.saigonplantravel.backend.trip.service;

import com.saigonplantravel.backend.trip.entity.Trip;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import com.saigonplantravel.backend.trip.repository.TripRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TripQueryService {

    private final TripRepository tripRepository;

    public TripQueryService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    public Trip findOwnedTrip(Long userId, UUID publicId) {
        return tripRepository.findByPublicIdAndUserId(publicId, userId).orElseThrow(TripNotFoundException::new);
    }
}
