package com.saigonplantravel.backend.place.scheduling;

import com.saigonplantravel.backend.place.domain.AdministrativeUnitType;
import com.saigonplantravel.backend.place.repository.PlaceRepository;
import com.saigonplantravel.backend.place.repository.projection.PlaceSchedulingBaseRow;
import com.saigonplantravel.backend.place.repository.projection.PlaceSchedulingCategoryRow;
import com.saigonplantravel.backend.place.repository.projection.PlaceSchedulingOpeningHourRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
@ExtendWith(MockitoExtension.class)
class PlaceSchedulingQueryServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    private PlaceSchedulingQueryService service;

    @BeforeEach
    void setUp() {
        service =
                new PlaceSchedulingQueryService(
                        placeRepository
                );
    }

    @Test
    void mapsOpeningHoursAndReturnsCandidatesInPlaceIdOrder() {
        // Arrange
        Set<Long> preferredCategoryIds =
                Set.of(5L, 2L);

        LocalDate tripDate =
                LocalDate.of(
                        2026,
                        8,
                        9
                );

        /*
         * Cố ý tạo base rows theo thứ tự 30, 10, 20
         * để kiểm tra service có sắp xếp output theo placeId hay không.
         */
        PlaceSchedulingBaseRow place30 =
                baseRow(
                        30L,
                        "Nhà hát C",
                        true
                );

        PlaceSchedulingBaseRow place10 =
                baseRow(
                        10L,
                        "Bảo tàng A",
                        true
                );

        PlaceSchedulingBaseRow place20 =
                baseRow(
                        20L,
                        "Công viên B",
                        false
                );

        /*
         * Phải tạo projection mock trước khi gọi
         * when(...).thenReturn(...).
         *
         * Không gọi categoryRow(...) trực tiếp bên trong thenReturn(),
         * vì helper này cũng sử dụng Mockito.when().
         */
        PlaceSchedulingCategoryRow place10Category2 =
                categoryRow(
                        10L,
                        2L
                );

        PlaceSchedulingCategoryRow place10Category5 =
                categoryRow(
                        10L,
                        5L
                );

        PlaceSchedulingCategoryRow place20Category2 =
                categoryRow(
                        20L,
                        2L
                );

        PlaceSchedulingCategoryRow place30Category5 =
                categoryRow(
                        30L,
                        5L
                );

        PlaceSchedulingOpeningHourRow place10OpeningHour =
                openingHourRow(
                        10L,
                        false,
                        LocalTime.of(8, 0),
                        LocalTime.of(17, 0)
                );

        PlaceSchedulingOpeningHourRow place20OpeningHour =
                openingHourRow(
                        20L,
                        true,
                        null,
                        null
                );

        /*
         * Query 1:
         * trả danh sách Place cơ bản.
         */
        when(
                placeRepository
                        .findSchedulingBaseRowsByPreferredCategoryIds(
                                eq(preferredCategoryIds)
                        )
        ).thenReturn(
                List.of(
                        place30,
                        place10,
                        place20
                )
        );

        /*
         * Query 2:
         * trả các category giao với sở thích của Trip.
         */
        when(
                placeRepository
                        .findSchedulingCategoryRowsByPreferredCategoryIds(
                                eq(preferredCategoryIds)
                        )
        ).thenReturn(
                List.of(
                        place10Category2,
                        place10Category5,
                        place20Category2,
                        place30Category5
                )
        );

        /*
         * Ngày 2026-08-09 là Chủ nhật.
         * ISO day of week của Chủ nhật là 7.
         *
         * Place 30 không có opening-hour row,
         * vì vậy service phải map Place 30 thành UNKNOWN.
         */
        when(
                placeRepository
                        .findSchedulingOpeningHourRows(
                                eq(
                                        Set.of(
                                                10L,
                                                20L,
                                                30L
                                        )
                                ),
                                eq((short) 7)
                        )
        ).thenReturn(
                List.of(
                        place10OpeningHour,
                        place20OpeningHour
                )
        );

        // Act
        List<PlaceSchedulingCandidate> candidates =
                service.findCandidates(
                        preferredCategoryIds,
                        tripDate
                );

        // Assert: output phải được sắp xếp theo placeId
        assertThat(candidates)
                .extracting(
                        PlaceSchedulingCandidate::placeId
                )
                .containsExactly(
                        10L,
                        20L,
                        30L
                );

        /*
         * Place 10:
         * - match Category 2 và 5
         * - có row mở cửa
         * - phải được map thành KNOWN_OPEN
         */
        PlaceSchedulingCandidate place10Candidate =
                findCandidate(
                        candidates,
                        10L
                );

        assertThat(
                place10Candidate
                        .matchedPreferredCategoryIds()
        ).containsExactly(
                2L,
                5L
        );

        assertThat(
                place10Candidate
                        .openingHours()
                        .status()
        ).isEqualTo(
                OpeningHoursStatus.KNOWN_OPEN
        );

        assertThat(
                place10Candidate
                        .openingHours()
                        .openTime()
        ).isEqualTo(
                LocalTime.of(8, 0)
        );

        assertThat(
                place10Candidate
                        .openingHours()
                        .closeTime()
        ).isEqualTo(
                LocalTime.of(17, 0)
        );

        /*
         * Place 20:
         * - có row với closed = true
         * - phải được map thành CLOSED
         */
        PlaceSchedulingCandidate place20Candidate =
                findCandidate(
                        candidates,
                        20L
                );

        assertThat(
                place20Candidate
                        .matchedPreferredCategoryIds()
        ).containsExactly(
                2L
        );

        assertThat(
                place20Candidate
                        .openingHours()
                        .status()
        ).isEqualTo(
                OpeningHoursStatus.CLOSED
        );

        assertThat(
                place20Candidate
                        .openingHours()
                        .openTime()
        ).isNull();

        assertThat(
                place20Candidate
                        .openingHours()
                        .closeTime()
        ).isNull();

        /*
         * Place 30:
         * - không có opening-hour row
         * - không được suy diễn thành CLOSED
         * - phải được map thành UNKNOWN
         */
        PlaceSchedulingCandidate place30Candidate =
                findCandidate(
                        candidates,
                        30L
                );

        assertThat(
                place30Candidate
                        .matchedPreferredCategoryIds()
        ).containsExactly(
                5L
        );

        assertThat(
                place30Candidate
                        .openingHours()
                        .status()
        ).isEqualTo(
                OpeningHoursStatus.UNKNOWN
        );

        assertThat(
                place30Candidate
                        .openingHours()
                        .openTime()
        ).isNull();

        assertThat(
                place30Candidate
                        .openingHours()
                        .closeTime()
        ).isNull();

        /*
         * Xác nhận service đã gọi đúng ba batch query.
         */
        verify(placeRepository)
                .findSchedulingBaseRowsByPreferredCategoryIds(
                        preferredCategoryIds
                );

        verify(placeRepository)
                .findSchedulingCategoryRowsByPreferredCategoryIds(
                        preferredCategoryIds
                );

        verify(placeRepository)
                .findSchedulingOpeningHourRows(
                        Set.of(
                                10L,
                                20L,
                                30L
                        ),
                        (short) 7
                );
    }
    @Test
    void returnsEmptyCandidatesWithoutCallingRepositoryWhenPreferencesAreEmpty() {
        // Arrange
        Set<Long> preferredCategoryIds =
                Set.of();

        LocalDate tripDate =
                LocalDate.of(
                        2026,
                        8,
                        9
                );

        // Act
        List<PlaceSchedulingCandidate> candidates =
                service.findCandidates(
                        preferredCategoryIds,
                        tripDate
                );

        // Assert
        assertThat(candidates)
                .isEmpty();

        verifyNoInteractions(
                placeRepository
        );
    }
    @Test
    void returnsEmptyCandidatesAndSkipsRemainingQueriesWhenNoBasePlaceExists() {
        // Arrange
        Set<Long> preferredCategoryIds =
                Set.of(
                        2L,
                        5L
                );

        LocalDate tripDate =
                LocalDate.of(
                        2026,
                        8,
                        9
                );

        when(
                placeRepository
                        .findSchedulingBaseRowsByPreferredCategoryIds(
                                eq(preferredCategoryIds)
                        )
        ).thenReturn(
                List.of()
        );

        // Act
        List<PlaceSchedulingCandidate> candidates =
                service.findCandidates(
                        preferredCategoryIds,
                        tripDate
                );

        // Assert
        assertThat(candidates)
                .isEmpty();

        verify(placeRepository)
                .findSchedulingBaseRowsByPreferredCategoryIds(
                        preferredCategoryIds
                );

        verifyNoMoreInteractions(
                placeRepository
        );
    }
    @Test
    void rejectsCategoryRowThatReferencesPlaceOutsideCandidatePool() {
        // Arrange
        Set<Long> preferredCategoryIds =
                Set.of(
                        2L,
                        5L
                );

        LocalDate tripDate =
                LocalDate.of(
                        2026,
                        8,
                        9
                );

        PlaceSchedulingBaseRow place10 =
                baseRowIdentity(10L);

        /*
         * Category row này không hợp lệ vì placeId 999
         * không tồn tại trong kết quả base query.
         */
        PlaceSchedulingCategoryRow unexpectedCategoryRow =
                categoryRow(
                        999L,
                        2L
                );

        when(
                placeRepository
                        .findSchedulingBaseRowsByPreferredCategoryIds(
                                eq(preferredCategoryIds)
                        )
        ).thenReturn(
                List.of(place10)
        );

        when(
                placeRepository
                        .findSchedulingCategoryRowsByPreferredCategoryIds(
                                eq(preferredCategoryIds)
                        )
        ).thenReturn(
                List.of(unexpectedCategoryRow)
        );

        /*
         * Service hiện query opening hours trước khi thực hiện
         * groupCategoryIdsByPlaceId(), nên test vẫn phải stub query này.
         */
        when(
                placeRepository
                        .findSchedulingOpeningHourRows(
                                eq(Set.of(10L)),
                                eq((short) 7)
                        )
        ).thenReturn(
                List.of()
        );

        // Act + Assert
        assertThatThrownBy(
                () -> service.findCandidates(
                        preferredCategoryIds,
                        tripDate
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "unexpected placeId=999"
                );
    }
    @Test
    void rejectsBasePlaceWithoutMatchedPreferredCategory() {
        // Arrange
        Set<Long> preferredCategoryIds =
                Set.of(
                        2L,
                        5L
                );

        LocalDate tripDate =
                LocalDate.of(
                        2026,
                        8,
                        9
                );

        PlaceSchedulingBaseRow place10 =
                baseRowIdentity(10L);

        when(
                placeRepository
                        .findSchedulingBaseRowsByPreferredCategoryIds(
                                eq(preferredCategoryIds)
                        )
        ).thenReturn(
                List.of(place10)
        );

        /*
         * Base query nói Place 10 là candidate,
         * nhưng category query không trả category nào.
         */
        when(
                placeRepository
                        .findSchedulingCategoryRowsByPreferredCategoryIds(
                                eq(preferredCategoryIds)
                        )
        ).thenReturn(
                List.of()
        );

        /*
         * Service hiện gọi opening-hours query trước khi map candidate,
         * nên vẫn phải stub lời gọi này.
         *
         * Không có opening-hour row sẽ được diễn giải là UNKNOWN,
         * nhưng service sẽ lỗi trước khi candidate được tạo vì thiếu category.
         */
        when(
                placeRepository
                        .findSchedulingOpeningHourRows(
                                eq(Set.of(10L)),
                                eq((short) 7)
                        )
        ).thenReturn(
                List.of()
        );

        // Act + Assert
        assertThatThrownBy(
                () -> service.findCandidates(
                        preferredCategoryIds,
                        tripDate
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "no matched preferred category"
                )
                .hasMessageContaining(
                        "placeId=10"
                );
    }
    @Test
    void rejectsDuplicateOpeningHourRowsForSamePlace() {
        // Arrange
        Set<Long> preferredCategoryIds =
                Set.of(2L);

        LocalDate tripDate =
                LocalDate.of(
                        2026,
                        8,
                        9
                );

        PlaceSchedulingBaseRow place10 =
                baseRowIdentity(10L);

        PlaceSchedulingCategoryRow place10Category2 =
                categoryRow(
                        10L,
                        2L
                );

        /*
         * Chỉ mock getPlaceId() vì service sẽ phát hiện
         * duplicate trước khi đọc closed/openTime/closeTime.
         *
         * Nếu dùng openingHourRow() hiện tại, Mockito strict mode
         * có thể báo UnnecessaryStubbingException.
         */
        PlaceSchedulingOpeningHourRow firstOpeningHourRow =
                mock(
                        PlaceSchedulingOpeningHourRow.class
                );

        when(firstOpeningHourRow.getPlaceId())
                .thenReturn(10L);

        PlaceSchedulingOpeningHourRow secondOpeningHourRow =
                mock(
                        PlaceSchedulingOpeningHourRow.class
                );

        when(secondOpeningHourRow.getPlaceId())
                .thenReturn(10L);

        when(
                placeRepository
                        .findSchedulingBaseRowsByPreferredCategoryIds(
                                eq(preferredCategoryIds)
                        )
        ).thenReturn(
                List.of(place10)
        );

        when(
                placeRepository
                        .findSchedulingCategoryRowsByPreferredCategoryIds(
                                eq(preferredCategoryIds)
                        )
        ).thenReturn(
                List.of(place10Category2)
        );

        when(
                placeRepository
                        .findSchedulingOpeningHourRows(
                                eq(Set.of(10L)),
                                eq((short) 7)
                        )
        ).thenReturn(
                List.of(
                        firstOpeningHourRow,
                        secondOpeningHourRow
                )
        );

        // Act + Assert
        assertThatThrownBy(
                () -> service.findCandidates(
                        preferredCategoryIds,
                        tripDate
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "Duplicate opening-hour rows"
                )
                .hasMessageContaining(
                        "placeId=10"
                );
    }
    private PlaceSchedulingBaseRow baseRowIdentity(
            Long placeId
    ) {
        PlaceSchedulingBaseRow row =
                mock(
                        PlaceSchedulingBaseRow.class
                );

        when(row.getPlaceId())
                .thenReturn(placeId);

        return row;
    }

    private PlaceSchedulingBaseRow baseRow(
            Long placeId,
            String name,
            boolean indoor
    ) {
        PlaceSchedulingBaseRow row =
                mock(
                        PlaceSchedulingBaseRow.class
                );

        when(row.getPlaceId())
                .thenReturn(placeId);

        when(row.getName())
                .thenReturn(name);

        when(row.getSlug())
                .thenReturn(
                        "place-" + placeId
                );

        when(row.getAddress())
                .thenReturn(
                        "Địa chỉ " + placeId
                );

        when(row.getAdministrativeUnitName())
                .thenReturn(
                        "Bến Nghé"
                );

        when(row.getAdministrativeUnitType())
                .thenReturn(
                        AdministrativeUnitType.WARD
                );

        when(row.getLatitude())
                .thenReturn(
                        new BigDecimal("10.7768890")
                );

        when(row.getLongitude())
                .thenReturn(
                        new BigDecimal("106.7008060")
                );

        when(row.getBaseVisitMinutes())
                .thenReturn(90);

        when(row.getMinCost())
                .thenReturn(
                        new BigDecimal("50000.00")
                );

        when(row.getIndoor())
                .thenReturn(indoor);

        return row;
    }

    private PlaceSchedulingCategoryRow categoryRow(
            Long placeId,
            Long categoryId
    ) {
        PlaceSchedulingCategoryRow row =
                mock(
                        PlaceSchedulingCategoryRow.class
                );

        when(row.getPlaceId())
                .thenReturn(placeId);

        when(row.getCategoryId())
                .thenReturn(categoryId);

        return row;
    }

    private PlaceSchedulingOpeningHourRow openingHourRow(
            Long placeId,
            boolean closed,
            LocalTime openTime,
            LocalTime closeTime
    ) {
        PlaceSchedulingOpeningHourRow row =
                mock(
                        PlaceSchedulingOpeningHourRow.class
                );

        when(row.getPlaceId())
                .thenReturn(placeId);

        when(row.getClosed())
                .thenReturn(closed);

        when(row.getOpenTime())
                .thenReturn(openTime);

        when(row.getCloseTime())
                .thenReturn(closeTime);

        /*
         * Không mock getDayOfWeek() vì service hiện tại
         * không đọc getter này.
         *
         * Nếu mock một method không được sử dụng,
         * Mockito strict mode có thể báo
         * UnnecessaryStubbingException.
         */

        return row;
    }

    private PlaceSchedulingCandidate findCandidate(
            List<PlaceSchedulingCandidate> candidates,
            Long placeId
    ) {
        return candidates.stream()
                .filter(
                        candidate ->
                                candidate
                                        .placeId()
                                        .equals(placeId)
                )
                .findFirst()
                .orElseThrow(
                        () -> new AssertionError(
                                "Missing candidate for placeId="
                                        + placeId
                        )
                );
    }
}
