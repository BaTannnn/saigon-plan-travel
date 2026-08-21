package com.saigonplantravel.backend.trip.repository;

import com.saigonplantravel.backend.trip.entity.Trip;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByPublicIdAndUserId(UUID publicId, Long userId);

    long countByUserIdAndTripDate(Long userId, LocalDate tripDate);

    List<Trip> findAllByUserIdOrderByTripDateAscStartTimeAscPublicIdAsc(Long userId);

    List<Trip> findAllByUserIdAndTripDateGreaterThanEqualAndTripDateLessThanOrderByTripDateAscStartTimeAscPublicIdAsc(
            Long userId, LocalDate startDate, LocalDate endDate);
}
