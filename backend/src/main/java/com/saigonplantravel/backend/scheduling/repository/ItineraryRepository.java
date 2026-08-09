package com.saigonplantravel.backend.scheduling.repository;

import com.saigonplantravel.backend.scheduling.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    Optional<Itinerary> findByPublicIdAndUserId(
            UUID publicId,
            Long userId
    );
}
