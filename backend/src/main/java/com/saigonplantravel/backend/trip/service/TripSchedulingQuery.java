package com.saigonplantravel.backend.trip.service;

import java.util.UUID;

public interface TripSchedulingQuery {

    TripSchedulingSnapshot getByPublicId(
            UUID publicId,
            Long userId
    );
}