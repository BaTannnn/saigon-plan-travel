package com.saigonplantravel.backend.trip.repository;

import com.saigonplantravel.backend.trip.entity.Trip;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripRepository
        extends JpaRepository<Trip, Long> {

    @EntityGraph(
            attributePaths = "preferredCategoryIds"
    )
    Optional<Trip> findByPublicIdAndUserId(
            UUID publicId,
            Long userId
    );

    @EntityGraph(
            attributePaths = "preferredCategoryIds"
    )
    List<Trip> findAllByUserIdOrderByTripDateAscStartTimeAscPublicIdAsc(
            Long userId
    );
}
