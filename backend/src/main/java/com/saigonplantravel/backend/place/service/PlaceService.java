package com.saigonplantravel.backend.place.service;

import com.saigonplantravel.backend.place.dto.OpeningHourState;
import com.saigonplantravel.backend.place.dto.PageResponse;
import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.PlaceQueryRequest;
import com.saigonplantravel.backend.place.dto.PlaceSummaryResponse;
import com.saigonplantravel.backend.place.dto.admin.AdminPlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.admin.AdminPlaceSummaryResponse;
import com.saigonplantravel.backend.place.dto.admin.PlaceCreateRequest;
import com.saigonplantravel.backend.place.dto.admin.PlaceOpeningHoursRequest;
import com.saigonplantravel.backend.place.dto.admin.PlaceUpdateRequest;
import com.saigonplantravel.backend.place.entity.Category;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.exception.InvalidPlaceCategoryAssignmentException;
import com.saigonplantravel.backend.place.exception.PlaceNotFoundException;
import com.saigonplantravel.backend.place.exception.PlaceSlugAlreadyExistsException;
import com.saigonplantravel.backend.place.mapper.PlaceMapper;
import com.saigonplantravel.backend.place.repository.CategoryRepository;
import com.saigonplantravel.backend.place.repository.PlaceRepository;
import com.saigonplantravel.backend.place.repository.specification.PlaceSpecifications;
import com.saigonplantravel.backend.place.search.PlaceSearchNormalizer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlaceService {

    private static final Sort PLACE_SORT = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));

    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;
    private final PlaceMapper placeMapper;

    public PlaceService(
            PlaceRepository placeRepository, CategoryRepository categoryRepository, PlaceMapper placeMapper) {
        this.placeRepository = placeRepository;
        this.categoryRepository = categoryRepository;
        this.placeMapper = placeMapper;
    }

    public PageResponse<PlaceSummaryResponse> findActivePlaces(PlaceQueryRequest request) {
        Page<PlaceSummaryResponse> result = placeRepository
                .findAll(
                        PlaceSpecifications.matching(request),
                        PageRequest.of(request.resolvedPage(), request.resolvedSize(), PLACE_SORT))
                .map(placeMapper::toSummaryResponse);

        return toPageResponse(result);
    }

    public PageResponse<AdminPlaceSummaryResponse> getPlacesForAdministration(String keyword, int page, int size) {
        String normalizedKeyword = PlaceSearchNormalizer.normalizeText(keyword);
        PageRequest pageRequest = PageRequest.of(page, size, PLACE_SORT);
        Page<Place> places = normalizedKeyword == null
                ? placeRepository.findAll(pageRequest)
                : placeRepository.findAll(PlaceSpecifications.adminKeyword(normalizedKeyword), pageRequest);
        Page<AdminPlaceSummaryResponse> result = places.map(placeMapper::toAdminSummaryResponse);

        return toPageResponse(result);
    }

    private <T> PageResponse<T> toPageResponse(Page<T> result) {
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast());
    }

    public PlaceDetailResponse getPlaceDetailBySlug(String slug) {
        return placeRepository
                .findBySlugAndActiveTrue(slug)
                .map(placeMapper::toDetailResponse)
                .orElseThrow(PlaceNotFoundException::new);
    }

    public AdminPlaceDetailResponse getPlaceDetailForAdministrationBySlug(String slug) {
        return placeRepository
                .findBySlug(slug)
                .map(placeMapper::toAdminDetailResponse)
                .orElseThrow(PlaceNotFoundException::new);
    }

    @Transactional
    public PlaceDetailResponse createPlace(PlaceCreateRequest request) {
        if (placeRepository.existsBySlug(request.slug())) {
            throw new PlaceSlugAlreadyExistsException();
        }

        Place place = new Place(
                request.name(),
                request.slug(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.estimatedVisitMinutes(),
                request.minCost(),
                request.maxCost(),
                request.indoor());
        place.updateDescriptions(request.shortDescription(), request.fullDescription());

        try {
            Place savedPlace = placeRepository.saveAndFlush(place);
            return placeMapper.toDetailResponse(savedPlace);
        } catch (DataIntegrityViolationException exception) {
            throw new PlaceSlugAlreadyExistsException();
        }
    }

    @Transactional
    public AdminPlaceDetailResponse updatePlace(String slug, PlaceUpdateRequest request) {
        Place place = placeRepository.findBySlug(slug).orElseThrow(PlaceNotFoundException::new);
        place.updateBasicInformation(
                request.name(),
                request.shortDescription(),
                request.fullDescription(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.estimatedVisitMinutes(),
                request.minCost(),
                request.maxCost(),
                request.indoor());
        return placeMapper.toAdminDetailResponse(place);
    }

    @Transactional
    public void activatePlace(String slug) {
        Place place = placeRepository.findBySlug(slug).orElseThrow(PlaceNotFoundException::new);
        place.activate();
    }

    @Transactional
    public void deactivatePlace(String slug) {
        Place place = placeRepository.findBySlug(slug).orElseThrow(PlaceNotFoundException::new);
        place.deactivate();
    }

    @Transactional
    public void replacePlaceCategories(String slug, List<String> requestedCategorySlugs) {
        Place place = placeRepository.findBySlug(slug).orElseThrow(PlaceNotFoundException::new);
        Set<String> requestedSlugs = Set.copyOf(requestedCategorySlugs);
        List<Category> categories = requestedSlugs.isEmpty()
                ? List.of()
                : categoryRepository.findAllBySlugInOrderByNameAscIdAsc(requestedSlugs);
        Set<String> resolvedSlugs = categories.stream().map(Category::getSlug).collect(Collectors.toSet());
        List<String> unknownSlugs = requestedSlugs.stream()
                .filter(requestedSlug -> !resolvedSlugs.contains(requestedSlug))
                .sorted()
                .toList();

        if (!unknownSlugs.isEmpty()) {
            throw new InvalidPlaceCategoryAssignmentException(unknownSlugs);
        }

        place.replaceCategories(categories);
    }

    @Transactional
    public void replacePlaceOpeningHours(String slug, List<PlaceOpeningHoursRequest.Day> days) {
        Place place = placeRepository.findBySlug(slug).orElseThrow(PlaceNotFoundException::new);

        for (PlaceOpeningHoursRequest.Day day : days) {
            if (day.getState() == OpeningHourState.UNKNOWN) {
                place.removeOpeningHour(day.getDayOfWeek());
            } else if (day.getState() == OpeningHourState.CLOSED) {
                place.markClosed(day.getDayOfWeek());
            } else {
                place.markOpen(day.getDayOfWeek(), day.getOpenTime(), day.getCloseTime());
            }
        }
    }
}
