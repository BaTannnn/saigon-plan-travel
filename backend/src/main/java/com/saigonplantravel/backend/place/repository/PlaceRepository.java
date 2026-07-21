package com.saigonplantravel.backend.place.repository;

import com.saigonplantravel.backend.place.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {

    Page<Place> findAllByActiveTrue(Pageable pageable);

    Optional<Place> findBySlugAndActiveTrue(String slug);
}
