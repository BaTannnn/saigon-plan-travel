package com.saigonplantravel.backend.itinerary.repository;

import com.saigonplantravel.backend.itinerary.entity.Itinerary;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    @EntityGraph(attributePaths = {"items", "items.place"})
    Optional<Itinerary> findByTripId(Long tripId);
}
