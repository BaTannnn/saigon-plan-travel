package com.saigonplantravel.backend.place.repository;

import com.saigonplantravel.backend.place.entity.Place;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PlaceRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {
    boolean existsBySlug(String slug);

    Page<Place> findAllByActiveTrue(Pageable pageable);

    Optional<Place> findBySlug(String slug);

    Optional<Place> findBySlugAndActiveTrue(String slug);

    List<Place> findAllBySlugInAndActiveTrue(Collection<String> slugs);
}
