package com.saigonplantravel.backend.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PlaceMapper placeMapper;

    @Test
    void searchesWithDefaultsFixedSortingAndSummaryMapping() {
        Place place = mock(Place.class);
        PlaceSummaryResponse summary = new PlaceSummaryResponse(
                1L,
                "Demo Place",
                "demo-place",
                null,
                new BigDecimal("10.0000000"),
                new BigDecimal("106.0000000"),
                60,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                true);
        when(placeRepository.findAll(
                        org.mockito.ArgumentMatchers.<Specification<Place>>any(),
                        org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    return new PageImpl<>(List.of(place), pageable, 1);
                });
        when(placeMapper.toSummaryResponse(place)).thenReturn(summary);

        PageResponse<PlaceSummaryResponse> response = new PlaceService(placeRepository, categoryRepository, placeMapper)
                .findActivePlaces(new PlaceQueryRequest("   ", null, null, new BigDecimal("100000"), null, null));

        assertThat(response.content()).containsExactly(summary);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(placeRepository)
                .findAll(org.mockito.ArgumentMatchers.<Specification<Place>>any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().toString()).isEqualTo("name: ASC,id: ASC");
    }

    @Test
    void searchesAdministrationAcrossAllStatusesWithAdminMapping() {
        Place place = mock(Place.class);
        AdminPlaceSummaryResponse summary = new AdminPlaceSummaryResponse(
                1L,
                "Inactive Place",
                "inactive-place",
                null,
                new BigDecimal("10.0000000"),
                new BigDecimal("106.0000000"),
                60,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                false,
                false);
        when(placeRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(0);
                    return new PageImpl<>(List.of(place), pageable, 1);
                });
        when(placeMapper.toAdminSummaryResponse(place)).thenReturn(summary);

        PageResponse<AdminPlaceSummaryResponse> response = new PlaceService(
                        placeRepository, categoryRepository, placeMapper)
                .getPlacesForAdministration("   ", 0, 20);

        assertThat(response.content()).containsExactly(summary);
        assertThat(response.content().getFirst().active()).isFalse();
        verify(placeMapper).toAdminSummaryResponse(place);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(placeRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().toString()).isEqualTo("name: ASC,id: ASC");
    }

    @Test
    void searchesAdministrationWithSpecificationAndStablePagination() {
        Place place = mock(Place.class);
        AdminPlaceSummaryResponse summary = new AdminPlaceSummaryResponse(
                1L,
                "Matching Place",
                "matching-place",
                null,
                new BigDecimal("10.0000000"),
                new BigDecimal("106.0000000"),
                60,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                true,
                false);
        when(placeRepository.findAll(
                        org.mockito.ArgumentMatchers.<Specification<Place>>any(),
                        org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    return new PageImpl<>(List.of(place), pageable, 1);
                });
        when(placeMapper.toAdminSummaryResponse(place)).thenReturn(summary);

        PageResponse<AdminPlaceSummaryResponse> response = new PlaceService(
                        placeRepository, categoryRepository, placeMapper)
                .getPlacesForAdministration("  MATCHING   PLACE ", 2, 10);

        assertThat(response.content()).containsExactly(summary);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(placeRepository)
                .findAll(org.mockito.ArgumentMatchers.<Specification<Place>>any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getValue().getSort().toString()).isEqualTo("name: ASC,id: ASC");
    }

    @Test
    void returnsDetailForExactActiveSlug() {
        Place place = mock(Place.class);
        PlaceDetailResponse detail = new PlaceDetailResponse(
                1L,
                "Demo Art Space",
                "demo-art-space",
                null,
                null,
                "Địa chỉ demo 1",
                new BigDecimal("10.7750000"),
                new BigDecimal("106.7000000"),
                90,
                new BigDecimal("50000.00"),
                new BigDecimal("150000.00"),
                true,
                List.of(),
                List.of());
        when(placeRepository.findBySlugAndActiveTrue("demo-art-space")).thenReturn(Optional.of(place));
        when(placeMapper.toDetailResponse(place)).thenReturn(detail);

        PlaceDetailResponse response = new PlaceService(placeRepository, categoryRepository, placeMapper)
                .getPlaceDetailBySlug("demo-art-space");

        assertThat(response).isSameAs(detail);
        verify(placeRepository).findBySlugAndActiveTrue("demo-art-space");
        verify(placeMapper).toDetailResponse(place);
    }

    @Test
    void returnsSameNotFoundForAnySlugWithoutActivePlace() {
        when(placeRepository.findBySlugAndActiveTrue("missing-slug")).thenReturn(Optional.empty());

        PlaceService service = new PlaceService(placeRepository, categoryRepository, placeMapper);

        assertThatThrownBy(() -> service.getPlaceDetailBySlug("missing-slug"))
                .isInstanceOf(PlaceNotFoundException.class)
                .hasMessage("Place not found");
    }

    @Test
    void returnsInactiveDetailForAdministration() {
        Place place = mock(Place.class);
        AdminPlaceDetailResponse detail = adminPlaceDetail(false);
        when(placeRepository.findBySlug("inactive-place")).thenReturn(Optional.of(place));
        when(placeMapper.toAdminDetailResponse(place)).thenReturn(detail);

        AdminPlaceDetailResponse response = new PlaceService(placeRepository, categoryRepository, placeMapper)
                .getPlaceDetailForAdministrationBySlug("inactive-place");

        assertThat(response).isSameAs(detail);
        assertThat(response.active()).isFalse();
    }

    @Test
    void updatesOnlyEditableBasicInformationAndPreservesIdentityAndStatus() {
        Place place = new Place(
                "Old Place",
                "immutable-slug",
                "Old address",
                new BigDecimal("10.7000000"),
                new BigDecimal("106.6000000"),
                60,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                true);
        place.deactivate();
        PlaceUpdateRequest request = new PlaceUpdateRequest(
                "  Updated Place  ",
                "  Updated short description  ",
                "   ",
                "  Updated address  ",
                new BigDecimal("10.7760000"),
                new BigDecimal("106.7010000"),
                120,
                new BigDecimal("60000.00"),
                new BigDecimal("160000.00"),
                false);
        AdminPlaceDetailResponse detail = adminPlaceDetail(false);
        when(placeRepository.findBySlug("immutable-slug")).thenReturn(Optional.of(place));
        when(placeMapper.toAdminDetailResponse(place)).thenReturn(detail);

        AdminPlaceDetailResponse response = new PlaceService(placeRepository, categoryRepository, placeMapper)
                .updatePlace("immutable-slug", request);

        assertThat(response).isSameAs(detail);
        assertThat(place.getName()).isEqualTo("Updated Place");
        assertThat(place.getSlug()).isEqualTo("immutable-slug");
        assertThat(place.getShortDescription()).isEqualTo("Updated short description");
        assertThat(place.getFullDescription()).isNull();
        assertThat(place.getAddress()).isEqualTo("Updated address");
        assertThat(place.getLatitude()).isEqualByComparingTo("10.7760000");
        assertThat(place.getLongitude()).isEqualByComparingTo("106.7010000");
        assertThat(place.getEstimatedVisitMinutes()).isEqualTo(120);
        assertThat(place.getMinCost()).isEqualByComparingTo("60000.00");
        assertThat(place.getMaxCost()).isEqualByComparingTo("160000.00");
        assertThat(place.getIndoor()).isFalse();
        assertThat(place.getActive()).isFalse();
        assertThat(place.getCategories()).isEmpty();
        assertThat(place.getOpeningHours()).isEmpty();
        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void rejectsUpdateWhenSlugDoesNotExist() {
        when(placeRepository.findBySlug("missing-place")).thenReturn(Optional.empty());

        PlaceService service = new PlaceService(placeRepository, categoryRepository, placeMapper);

        assertThatThrownBy(() -> service.updatePlace("missing-place", validUpdateRequest()))
                .isInstanceOf(PlaceNotFoundException.class)
                .hasMessage("Place not found");
    }

    @Test
    void activatesInactivePlaceWithoutExplicitSave() {
        Place place = new Place(
                "Inactive Place",
                "inactive-place",
                "Address",
                new BigDecimal("10.7000000"),
                new BigDecimal("106.6000000"),
                60,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false);
        place.deactivate();
        when(placeRepository.findBySlug("inactive-place")).thenReturn(Optional.of(place));

        new PlaceService(placeRepository, categoryRepository, placeMapper).activatePlace("inactive-place");

        assertThat(place.getActive()).isTrue();
        assertThat(place.getSlug()).isEqualTo("inactive-place");
        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void deactivatesActivePlaceWithoutExplicitSave() {
        Place place = new Place(
                "Active Place",
                "active-place",
                "Address",
                new BigDecimal("10.7000000"),
                new BigDecimal("106.6000000"),
                60,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false);
        when(placeRepository.findBySlug("active-place")).thenReturn(Optional.of(place));

        new PlaceService(placeRepository, categoryRepository, placeMapper).deactivatePlace("active-place");

        assertThat(place.getActive()).isFalse();
        assertThat(place.getSlug()).isEqualTo("active-place");
        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void rejectsStatusChangeWhenSlugDoesNotExist() {
        when(placeRepository.findBySlug("missing-place")).thenReturn(Optional.empty());

        PlaceService service = new PlaceService(placeRepository, categoryRepository, placeMapper);

        assertThatThrownBy(() -> service.activatePlace("missing-place"))
                .isInstanceOf(PlaceNotFoundException.class)
                .hasMessage("Place not found");
    }

    @Test
    void replacesPlaceCategoriesWithResolvedCatalogEntries() {
        Place place = place("category-place");
        Category art = mock(Category.class);
        Category culture = mock(Category.class);
        when(art.getSlug()).thenReturn("art");
        when(culture.getSlug()).thenReturn("culture");
        when(placeRepository.findBySlug("category-place")).thenReturn(Optional.of(place));
        when(categoryRepository.findAllBySlugInOrderByNameAscIdAsc(Set.of("art", "culture")))
                .thenReturn(List.of(art, culture));

        new PlaceService(placeRepository, categoryRepository, placeMapper)
                .replacePlaceCategories("category-place", List.of("culture", "art"));

        assertThat(place.getCategories()).containsExactlyInAnyOrder(art, culture);
    }

    @Test
    void allowsRemovingEveryPlaceCategoryWithoutCatalogQuery() {
        Place place = place("category-place");
        Category existing = mock(Category.class);
        place.replaceCategories(List.of(existing));
        when(placeRepository.findBySlug("category-place")).thenReturn(Optional.of(place));

        new PlaceService(placeRepository, categoryRepository, placeMapper)
                .replacePlaceCategories("category-place", List.of());

        assertThat(place.getCategories()).isEmpty();
        verify(categoryRepository, never()).findAllBySlugInOrderByNameAscIdAsc(any());
    }

    @Test
    void rejectsUnknownCategoryWithoutChangingExistingAssignments() {
        Place place = place("category-place");
        Category existing = mock(Category.class);
        Category art = mock(Category.class);
        when(art.getSlug()).thenReturn("art");
        place.replaceCategories(List.of(existing));
        when(placeRepository.findBySlug("category-place")).thenReturn(Optional.of(place));
        when(categoryRepository.findAllBySlugInOrderByNameAscIdAsc(Set.of("art", "missing")))
                .thenReturn(List.of(art));

        PlaceService service = new PlaceService(placeRepository, categoryRepository, placeMapper);

        assertThatThrownBy(() -> service.replacePlaceCategories("category-place", List.of("art", "missing")))
                .isInstanceOf(InvalidPlaceCategoryAssignmentException.class)
                .hasMessage("One or more selected categories do not exist");
        assertThat(place.getCategories()).containsExactly(existing);
    }

    @Test
    void replacesOpeningHoursAcrossUnknownClosedAndOpenStates() {
        Place place = place("opening-hours-place");
        place.markOpen((short) 1, LocalTime.of(8, 0), LocalTime.of(16, 0));
        place.markClosed((short) 2);
        when(placeRepository.findBySlug("opening-hours-place")).thenReturn(Optional.of(place));
        List<PlaceOpeningHoursRequest.Day> days = List.of(
                new PlaceOpeningHoursRequest.Day((short) 1, OpeningHourState.CLOSED, null, null),
                new PlaceOpeningHoursRequest.Day((short) 2, OpeningHourState.UNKNOWN, null, null),
                new PlaceOpeningHoursRequest.Day(
                        (short) 3, OpeningHourState.OPEN, LocalTime.of(9, 30), LocalTime.of(18, 0)));

        new PlaceService(placeRepository, categoryRepository, placeMapper)
                .replacePlaceOpeningHours("opening-hours-place", days);

        assertThat(place.getOpeningHours()).hasSize(2);
        assertThat(place.getOpeningHours())
                .filteredOn(openingHour -> openingHour.getDayOfWeek() == 1)
                .singleElement()
                .satisfies(openingHour -> {
                    assertThat(openingHour.getClosed()).isTrue();
                    assertThat(openingHour.getOpenTime()).isNull();
                    assertThat(openingHour.getCloseTime()).isNull();
                });
        assertThat(place.getOpeningHours())
                .filteredOn(openingHour -> openingHour.getDayOfWeek() == 3)
                .singleElement()
                .satisfies(openingHour -> {
                    assertThat(openingHour.getClosed()).isFalse();
                    assertThat(openingHour.getOpenTime()).isEqualTo(LocalTime.of(9, 30));
                    assertThat(openingHour.getCloseTime()).isEqualTo(LocalTime.of(18, 0));
                });
    }

    @Test
    void createsActivePlaceWithNormalizedBasicInformation() {
        PlaceCreateRequest request = new PlaceCreateRequest(
                "  New Place  ",
                "  new-place  ",
                "  Short description  ",
                "   ",
                "  New address  ",
                new BigDecimal("10.7750000"),
                new BigDecimal("106.7000000"),
                90,
                new BigDecimal("50000.00"),
                new BigDecimal("150000.00"),
                true);
        PlaceDetailResponse detail = new PlaceDetailResponse(
                1L,
                "New Place",
                "new-place",
                "Short description",
                null,
                "New address",
                new BigDecimal("10.7750000"),
                new BigDecimal("106.7000000"),
                90,
                new BigDecimal("50000.00"),
                new BigDecimal("150000.00"),
                true,
                List.of(),
                List.of());
        when(placeRepository.existsBySlug("new-place")).thenReturn(false);
        when(placeRepository.saveAndFlush(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(placeMapper.toDetailResponse(any(Place.class))).thenReturn(detail);

        PlaceDetailResponse response =
                new PlaceService(placeRepository, categoryRepository, placeMapper).createPlace(request);

        assertThat(response).isSameAs(detail);
        ArgumentCaptor<Place> placeCaptor = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).saveAndFlush(placeCaptor.capture());
        Place savedPlace = placeCaptor.getValue();
        assertThat(savedPlace.getName()).isEqualTo("New Place");
        assertThat(savedPlace.getSlug()).isEqualTo("new-place");
        assertThat(savedPlace.getShortDescription()).isEqualTo("Short description");
        assertThat(savedPlace.getFullDescription()).isNull();
        assertThat(savedPlace.getAddress()).isEqualTo("New address");
        assertThat(savedPlace.getActive()).isTrue();
        assertThat(savedPlace.getCategories()).isEmpty();
        assertThat(savedPlace.getOpeningHours()).isEmpty();
    }

    @Test
    void rejectsExistingSlugBeforeSaving() {
        PlaceCreateRequest request = validCreateRequest();
        when(placeRepository.existsBySlug("new-place")).thenReturn(true);

        PlaceService service = new PlaceService(placeRepository, categoryRepository, placeMapper);

        assertThatThrownBy(() -> service.createPlace(request))
                .isInstanceOf(PlaceSlugAlreadyExistsException.class)
                .hasMessage("Slug already exists");
        verify(placeRepository, never()).saveAndFlush(any(Place.class));
    }

    @Test
    void mapsDatabaseUniquenessRaceToDuplicateSlugFailure() {
        PlaceCreateRequest request = validCreateRequest();
        when(placeRepository.existsBySlug("new-place")).thenReturn(false);
        when(placeRepository.saveAndFlush(any(Place.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        PlaceService service = new PlaceService(placeRepository, categoryRepository, placeMapper);

        assertThatThrownBy(() -> service.createPlace(request))
                .isInstanceOf(PlaceSlugAlreadyExistsException.class)
                .hasMessage("Slug already exists");
    }

    private PlaceCreateRequest validCreateRequest() {
        return new PlaceCreateRequest(
                "New Place",
                "new-place",
                null,
                null,
                "New address",
                new BigDecimal("10.7750000"),
                new BigDecimal("106.7000000"),
                90,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                false);
    }

    private PlaceUpdateRequest validUpdateRequest() {
        return new PlaceUpdateRequest(
                "Updated Place",
                null,
                null,
                "Updated address",
                new BigDecimal("10.7750000"),
                new BigDecimal("106.7000000"),
                90,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                false);
    }

    private AdminPlaceDetailResponse adminPlaceDetail(boolean active) {
        return new AdminPlaceDetailResponse(
                1L,
                "Updated Place",
                "immutable-slug",
                "Updated short description",
                null,
                "Updated address",
                new BigDecimal("10.7760000"),
                new BigDecimal("106.7010000"),
                120,
                new BigDecimal("60000.00"),
                new BigDecimal("160000.00"),
                false,
                List.of(),
                List.of(),
                active);
    }

    private Place place(String slug) {
        return new Place(
                "Category Place",
                slug,
                "Address",
                new BigDecimal("10.7000000"),
                new BigDecimal("106.6000000"),
                60,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false);
    }
}
