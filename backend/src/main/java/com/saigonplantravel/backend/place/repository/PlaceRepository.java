package com.saigonplantravel.backend.place.repository;

import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.repository.projection.PlaceSchedulingBaseRow;
import com.saigonplantravel.backend.place.repository.projection.PlaceSchedulingCategoryRow;
import com.saigonplantravel.backend.place.repository.projection.PlaceSchedulingOpeningHourRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {
    @Query("""
        SELECT DISTINCT
            place.id AS placeId,
            place.name AS name,
            place.slug AS slug,
            place.address AS address,
            place.administrativeUnitName
                AS administrativeUnitName,
            place.administrativeUnitType
                AS administrativeUnitType,
            place.latitude AS latitude,
            place.longitude AS longitude,
            place.estimatedVisitMinutes
                AS baseVisitMinutes,
            place.minCost AS minCost,
            place.indoor AS indoor
        FROM Place place
        JOIN place.categories category
        WHERE place.active = true
          AND category.id IN :preferredCategoryIds
        ORDER BY place.id ASC
        """)
    List<PlaceSchedulingBaseRow>
    findSchedulingBaseRowsByPreferredCategoryIds(
            @Param("preferredCategoryIds")
            Collection<Long> preferredCategoryIds
    );
    @Query("""
        SELECT
            place.id AS placeId,
            category.id AS categoryId
        FROM Place place
        JOIN place.categories category
        WHERE place.active = true
          AND category.id IN :preferredCategoryIds
        ORDER BY place.id ASC, category.id ASC
        """)
    List<PlaceSchedulingCategoryRow>
    findSchedulingCategoryRowsByPreferredCategoryIds(
            @Param("preferredCategoryIds")
            Collection<Long> preferredCategoryIds
    );
    @Query("""
        SELECT
            openingHour.place.id AS placeId,
            openingHour.dayOfWeek AS dayOfWeek,
            openingHour.openTime AS openTime,
            openingHour.closeTime AS closeTime,
            openingHour.closed AS closed
        FROM OpeningHour openingHour
        WHERE openingHour.place.id IN :placeIds
          AND openingHour.place.active = true
          AND openingHour.dayOfWeek = :dayOfWeek
        ORDER BY openingHour.place.id ASC
        """)
    List<PlaceSchedulingOpeningHourRow>
    findSchedulingOpeningHourRows(
            @Param("placeIds")
            Collection<Long> placeIds,

            @Param("dayOfWeek")
            Short dayOfWeek
    );
    Page<Place> findAllByActiveTrue(Pageable pageable);

    Optional<Place> findBySlugAndActiveTrue(String slug);
}
