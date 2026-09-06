package com.saigonplantravel.backend.place.repository;

import com.saigonplantravel.backend.place.entity.Place;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PlaceRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {
    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = "coverImage")
    Optional<Place> findBySlug(String slug);

    @EntityGraph(attributePaths = "coverImage")
    Optional<Place> findBySlugAndActiveTrue(String slug);

    @EntityGraph(attributePaths = {"coverImage", "openingHours"})
    @Query("select distinct place from Place place where place.slug in :slugs and place.active = true")
    List<Place> findAllActiveBySlugsForScheduling(Collection<String> slugs);

    @Override
    @EntityGraph(attributePaths = "coverImage")
    Optional<Place> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "coverImage")
    Page<Place> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "coverImage")
    Page<Place> findAll(Specification<Place> specification, Pageable pageable);
}
