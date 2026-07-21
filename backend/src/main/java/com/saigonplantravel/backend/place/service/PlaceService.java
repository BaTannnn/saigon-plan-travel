package com.saigonplantravel.backend.place.service;

import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.PlacePageResponse;
import com.saigonplantravel.backend.place.dto.PlaceSearchRequest;
import com.saigonplantravel.backend.place.dto.PlaceSummaryResponse;
import com.saigonplantravel.backend.place.exception.PlaceNotFoundException;
import com.saigonplantravel.backend.place.mapper.PlaceMapper;
import com.saigonplantravel.backend.place.repository.PlaceRepository;
import com.saigonplantravel.backend.place.repository.specification.PlaceSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlaceService {

    private static final Sort PLACE_SORT = Sort.by(
            Sort.Order.asc("name"),
            Sort.Order.asc("id")
    );

    private final PlaceRepository placeRepository;
    private final PlaceMapper placeMapper;

    public PlaceService(PlaceRepository placeRepository, PlaceMapper placeMapper) {
        this.placeRepository = placeRepository;
        this.placeMapper = placeMapper;
    }

    public PlacePageResponse getActivePlaces(int page, int size) {
        Page<PlaceSummaryResponse> result = placeRepository
                .findAllByActiveTrue(PageRequest.of(page, size, PLACE_SORT))
                .map(placeMapper::toSummaryResponse);

        return toPageResponse(result);
    }

    public PlacePageResponse searchPlaces(PlaceSearchRequest request) {
        Page<PlaceSummaryResponse> result = placeRepository.findAll(
                        PlaceSpecifications.matching(request),
                        PageRequest.of(request.resolvedPage(), request.resolvedSize(), PLACE_SORT)
                )
                .map(placeMapper::toSummaryResponse);

        return toPageResponse(result);
    }

    private PlacePageResponse toPageResponse(Page<PlaceSummaryResponse> result) {
        return new PlacePageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    public PlaceDetailResponse getPlaceDetailBySlug(String slug) {
        return placeRepository.findBySlugAndActiveTrue(slug)
                .map(placeMapper::toDetailResponse)
                .orElseThrow(PlaceNotFoundException::new);
    }
}
