package com.saigonplantravel.backend.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.PlacePageResponse;
import com.saigonplantravel.backend.place.dto.PlaceSearchRequest;
import com.saigonplantravel.backend.place.dto.PlaceSummaryResponse;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.exception.PlaceNotFoundException;
import com.saigonplantravel.backend.place.mapper.PlaceMapper;
import com.saigonplantravel.backend.place.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceMapper placeMapper;

    @Test
    void mapsPageAndUsesLockedSorting() {
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
        when(placeRepository.findAllByActiveTrue(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(0);
                    return new PageImpl<>(List.of(place), pageable, 1);
                });
        when(placeMapper.toSummaryResponse(place)).thenReturn(summary);

        PlacePageResponse response = new PlaceService(placeRepository, placeMapper).getActivePlaces(0, 20);

        assertThat(response.content()).containsExactly(summary);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(placeRepository).findAllByActiveTrue(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().toString()).isEqualTo("name: ASC,id: ASC");
    }

    @Test
    void preservesEmptyPageEnvelope() {
        Pageable pageable = PageRequest.of(3, 10, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));
        when(placeRepository.findAllByActiveTrue(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PlacePageResponse response = new PlaceService(placeRepository, placeMapper).getActivePlaces(3, 10);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(3);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isTrue();
    }

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

        PlacePageResponse response = new PlaceService(placeRepository, placeMapper)
                .searchPlaces(new PlaceSearchRequest("   ", null, null, new BigDecimal("100000"), null, null));

        assertThat(response.content()).containsExactly(summary);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(placeRepository)
                .findAll(org.mockito.ArgumentMatchers.<Specification<Place>>any(), pageableCaptor.capture());
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

        PlaceDetailResponse response =
                new PlaceService(placeRepository, placeMapper).getPlaceDetailBySlug("demo-art-space");

        assertThat(response).isSameAs(detail);
        verify(placeRepository).findBySlugAndActiveTrue("demo-art-space");
        verify(placeMapper).toDetailResponse(place);
    }

    @Test
    void returnsSameNotFoundForAnySlugWithoutActivePlace() {
        when(placeRepository.findBySlugAndActiveTrue("missing-slug")).thenReturn(Optional.empty());

        PlaceService service = new PlaceService(placeRepository, placeMapper);

        assertThatThrownBy(() -> service.getPlaceDetailBySlug("missing-slug"))
                .isInstanceOf(PlaceNotFoundException.class)
                .hasMessage("Place not found");
    }
}
