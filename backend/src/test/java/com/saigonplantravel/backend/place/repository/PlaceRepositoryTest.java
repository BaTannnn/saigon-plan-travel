package com.saigonplantravel.backend.place.repository;

import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.PlacePageResponse;
import com.saigonplantravel.backend.place.dto.PlaceSearchRequest;
import com.saigonplantravel.backend.place.entity.Category;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.repository.projection.PlaceSchedulingOpeningHourRow;
import com.saigonplantravel.backend.place.service.PlaceService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class PlaceRepositoryTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add(
                "app.security.jwt.secret",
                () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        );
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PlaceService placeService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsOnlyActivePlacesWithStableSortingAndPagination() {
        Sort sort = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));

        Page<Place> firstPage = placeRepository.findAllByActiveTrue(PageRequest.of(0, 2, sort));
        Page<Place> finalPage = placeRepository.findAllByActiveTrue(PageRequest.of(2, 2, sort));

        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.getContent())
                .extracting(Place::getName)
                .containsExactly("Demo Art Space", "Demo City Garden");
        assertThat(finalPage.getContent())
                .extracting(Place::getName)
                .containsExactly("Demo Science Center");

        List<String> returnedSlugs = placeRepository
                .findAllByActiveTrue(PageRequest.of(0, 100, sort))
                .map(Place::getSlug)
                .getContent();
        assertThat(returnedSlugs).doesNotContain("demo-temporarily-hidden-place");
        assertThat(returnedSlugs).hasSize(5);
        assertThat(firstPage.getContent())
                .allSatisfy(place -> assertThat(place.getActive()).isTrue());
    }

    @Test
    void rejectsInvalidPlaceDataWithCanonicalConstraints() {
        assertInvalidPlace("invalid-latitude", new BigDecimal("91"), new BigDecimal("106"),
                60, BigDecimal.ZERO, BigDecimal.ZERO, true);
        assertInvalidPlace("invalid-longitude", new BigDecimal("10"), new BigDecimal("181"),
                60, BigDecimal.ZERO, BigDecimal.ZERO, true);
        assertInvalidPlace("invalid-duration", new BigDecimal("10"), new BigDecimal("106"),
                0, BigDecimal.ZERO, BigDecimal.ZERO, true);
        assertInvalidPlace("invalid-min-cost", new BigDecimal("10"), new BigDecimal("106"),
                60, new BigDecimal("-1"), BigDecimal.ZERO, true);
        assertInvalidPlace("invalid-max-cost", new BigDecimal("10"), new BigDecimal("106"),
                60, new BigDecimal("100"), new BigDecimal("99"), true);
        assertInvalidPlace("invalid-indoor", new BigDecimal("10"), new BigDecimal("106"),
                60, BigDecimal.ZERO, BigDecimal.ZERO, null);
        assertInvalidPlace("demo-art-space", new BigDecimal("10"), new BigDecimal("106"),
                60, BigDecimal.ZERO, BigDecimal.ZERO, true);
        assertInvalidSql("""
                INSERT INTO places (
                    name, slug, address, administrative_unit_name,
                    latitude, longitude, estimated_visit_minutes,
                    min_cost, max_cost, indoor, active
                ) VALUES (
                    'Missing unit type', 'missing-unit-type', 'Demo address', 'Đơn vị Demo',
                    10.7, 106.6, 60, 0, 0, TRUE, TRUE
                )
                """);
        assertInvalidSql("""
                INSERT INTO places (
                    name, slug, address, administrative_unit_name, administrative_unit_type,
                    latitude, longitude, estimated_visit_minutes,
                    min_cost, max_cost, indoor, active
                ) VALUES (
                    'Blank unit name', 'blank-unit-name', 'Demo address', '   ', 'WARD',
                    10.7, 106.6, 60, 0, 0, TRUE, TRUE
                )
                """);
        assertInvalidSql("""
                INSERT INTO places (
                    name, slug, address, administrative_unit_name, administrative_unit_type,
                    latitude, longitude, estimated_visit_minutes,
                    min_cost, max_cost, indoor, active
                ) VALUES (
                    'Invalid unit type', 'invalid-unit-type', 'Demo address', 'Đơn vị Demo', 'CITY',
                    10.7, 106.6, 60, 0, 0, TRUE, TRUE
                )
                """);
    }

    @Test
    void findsOnlyExactActiveSlug() {
        assertThat(placeRepository.findBySlugAndActiveTrue("demo-art-space")).isPresent();
        assertThat(placeRepository.findBySlugAndActiveTrue("demo-temporarily-hidden-place"))
                .isEmpty();
        assertThat(placeRepository.findBySlugAndActiveTrue("DEMO-ART-SPACE")).isEmpty();
        assertThat(placeRepository.findBySlugAndActiveTrue(" demo-art-space ")).isEmpty();
    }

    @Test
    void returnsAllCategoriesInStableOrder() {
        assertThat(categoryRepository.findAllByOrderByNameAscIdAsc())
                .extracting(Category::getName)
                .containsExactly("Khoa học", "Lịch sử", "Nghệ thuật", "Ngoài trời", "Văn hóa");
    }

    @Test
    void rejectsInvalidCategoryAndOpeningHourStates() {
        assertInvalidSql("""
                INSERT INTO categories (name, slug)
                VALUES ('Invalid uppercase', 'Invalid-Slug')
                """);
        assertInvalidSql("""
                INSERT INTO categories (name, slug)
                VALUES ('Invalid double hyphen', 'invalid--slug')
                """);
        assertInvalidSql("""
                INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, closed)
                SELECT id, 1, TIME '09:00', TIME '17:00', TRUE
                FROM places WHERE slug = 'demo-temporarily-hidden-place'
                """);
        assertInvalidSql("""
                INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, closed)
                SELECT id, 2, TIME '09:00', NULL, FALSE
                FROM places WHERE slug = 'demo-temporarily-hidden-place'
                """);
        assertInvalidSql("""
                INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, closed)
                SELECT id, 3, TIME '17:00', TIME '09:00', FALSE
                FROM places WHERE slug = 'demo-temporarily-hidden-place'
                """);
        assertInvalidSql("""
                INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, closed)
                SELECT id, 8, TIME '09:00', TIME '17:00', FALSE
                FROM places WHERE slug = 'demo-temporarily-hidden-place'
                """);
        assertInvalidSql("""
                INSERT INTO place_categories (place_id, category_id)
                SELECT place.id, category.id
                FROM places place, categories category
                WHERE place.slug = 'demo-art-space'
                  AND category.slug = 'nghe-thuat'
                """);
        assertInvalidSql("""
                INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, closed)
                SELECT id, 1, TIME '09:00', TIME '17:00', FALSE
                FROM places WHERE slug = 'demo-art-space'
                """);
    }

    @Test
    void loadsDetailInAtMostThreeQueriesWithoutInventingUnknownDays() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        PlaceDetailResponse artSpace = placeService.getPlaceDetailBySlug("demo-art-space");

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        assertThat(artSpace.categories())
                .extracting(category -> category.name())
                .containsExactly("Nghệ thuật", "Văn hóa");
        assertThat(artSpace.openingHours()).hasSize(7);
        assertThat(artSpace.openingHours().get(1).closed()).isTrue();
        assertThat(artSpace.openingHours().get(1).openTime()).isNull();
        assertThat(artSpace.openingHours().get(1).closeTime()).isNull();

        statistics.clear();
        PlaceDetailResponse cityGarden = placeService.getPlaceDetailBySlug("demo-city-garden");

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        assertThat(cityGarden.openingHours())
                .extracting(openingHour -> openingHour.dayOfWeek())
                .containsExactly((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);
    }

    @Test
    @Transactional
    void searchesKeywordAcrossEveryTextFieldWithoutCaseOrVietnameseDiacritics() {
        insertSearchPlace(
                "search-name",
                "Bảo tàng Name",
                null,
                null,
                "Địa chỉ demo",
                "Đơn vị Demo",
                true
        );
        insertSearchPlace(
                "search-short-description",
                "Search Short",
                "Không gian Bảo tàng",
                null,
                "Địa chỉ demo",
                "Đơn vị Demo",
                true
        );
        insertSearchPlace(
                "search-full-description",
                "Search Full",
                null,
                "Nội dung Bảo tàng",
                "Địa chỉ demo",
                "Đơn vị Demo",
                true
        );
        insertSearchPlace(
                "search-address",
                "Search Address",
                null,
                null,
                "Đường Bảo tàng",
                "Đơn vị Demo",
                true
        );
        insertSearchPlace(
                "search-administrative-unit",
                "Search Administrative Unit",
                null,
                null,
                "Địa chỉ demo",
                "Đơn vị Bảo tàng",
                true
        );

        PlacePageResponse response = placeService.searchPlaces(searchRequest(
                "BAO TANG",
                null,
                null,
                null,
                null,
                0,
                100
        ));

        assertThat(response.content())
                .extracting(item -> item.slug())
                .containsExactlyInAnyOrder(
                        "search-name",
                        "search-short-description",
                        "search-full-description",
                        "search-address",
                        "search-administrative-unit"
                );
    }

    @Test
    @Transactional
    void treatsLikeWildcardsAsLiteralCharacters() {
        insertSearchPlace(
                "literal-wildcards",
                "Demo 50%_path\\name",
                null,
                null,
                "Địa chỉ demo",
                "Đơn vị Demo",
                true
        );

        PlacePageResponse response = placeService.searchPlaces(searchRequest(
                "50%_path\\name",
                null,
                null,
                null,
                null,
                0,
                100
        ));

        assertThat(response.content())
                .extracting(item -> item.slug())
                .containsExactly("literal-wildcards");
    }

    @Test
    @Transactional
    void combinesAllFiltersAndExcludesInactivePlaces() {
        assignAdministrativeUnit(
                "Đơn vị Demo",
                "demo-art-space",
                "demo-history-hall",
                "demo-temporarily-hidden-place"
        );

        PlacePageResponse response = placeService.searchPlaces(searchRequest(
                "demo",
                "don vi demo",
                "van-hoa",
                true,
                new BigDecimal("100000"),
                0,
                100
        ));

        assertThat(response.content())
                .extracting(item -> item.slug())
                .containsExactly("demo-art-space", "demo-history-hall")
                .doesNotContain("demo-temporarily-hidden-place");
        assertThat(response.totalElements()).isEqualTo(2);
    }

    @Test
    @Transactional
    void appliesAdministrativeUnitCategoryIndoorAndBudgetSemanticsIndividually() {
        assignAdministrativeUnit("Đơn vị Demo", "demo-art-space", "demo-history-hall");

        assertThat(placeService.searchPlaces(searchRequest(
                null, "Demo", null, null, null, 0, 100
        )).content()).isEmpty();

        assertThat(placeService.searchPlaces(searchRequest(
                null, "don vi demo", null, null, null, 0, 100
        )).content())
                .extracting(item -> item.slug())
                .containsExactly("demo-art-space", "demo-history-hall");

        assertThat(placeService.searchPlaces(searchRequest(
                null, null, "khong-ton-tai", null, null, 0, 100
        )).content()).isEmpty();

        assertThat(placeService.searchPlaces(searchRequest(
                null, null, null, false, null, 0, 100
        )).content())
                .extracting(item -> item.slug())
                .containsExactly("demo-city-garden", "demo-riverside-walk");

        assertThat(placeService.searchPlaces(searchRequest(
                null, null, null, null, BigDecimal.ZERO, 0, 100
        )).content())
                .extracting(item -> item.slug())
                .containsExactly("demo-city-garden", "demo-riverside-walk");
    }

    @Test
    void usesAtMostContentAndCountQueriesForCategoryPagination() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        PlacePageResponse response = placeService.searchPlaces(searchRequest(
                null,
                null,
                "van-hoa",
                null,
                null,
                0,
                1
        ));

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 2L);
    }

    @Test
    @Transactional
    void keepsPaginationStableAndFastWithThirtyMatchingPlaces() {
        for (int index = 0; index < 30; index++) {
            insertSearchPlace(
                    "performance-fixture-" + index,
                    "Performance Fixture",
                    "Fixture search target",
                    null,
                    "Test address " + index,
                    "Đơn vị Demo",
                    true
            );
        }

        PlaceSearchRequest firstPageRequest = searchRequest(
                "fixture search target", null, null, null, null, 0, 10
        );
        PlaceSearchRequest secondPageRequest = searchRequest(
                "fixture search target", null, null, null, null, 1, 10
        );
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        statistics.clear();
        long firstPageStartedAt = System.nanoTime();
        PlacePageResponse firstPage = placeService.searchPlaces(firstPageRequest);
        long firstPageElapsedNanos = System.nanoTime() - firstPageStartedAt;
        assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 2L);

        statistics.clear();
        long secondPageStartedAt = System.nanoTime();
        PlacePageResponse secondPage = placeService.searchPlaces(secondPageRequest);
        long secondPageElapsedNanos = System.nanoTime() - secondPageStartedAt;
        assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 2L);

        assertThat(firstPage.totalElements()).isEqualTo(30);
        assertThat(firstPage.totalPages()).isEqualTo(3);
        assertThat(firstPage.content()).hasSize(10);
        assertThat(secondPage.content()).hasSize(10);
        assertThat(firstPage.content())
                .extracting(item -> item.id())
                .isSorted()
                .doesNotContainAnyElementsOf(
                        secondPage.content().stream().map(item -> item.id()).toList()
                );
        assertThat(secondPage.content())
                .extracting(item -> item.id())
                .isSorted();
        assertThat(firstPageElapsedNanos).isLessThan(500_000_000L);
        assertThat(secondPageElapsedNanos).isLessThan(500_000_000L);
    }
    @Test
    void loadsOpeningHoursForRequestedPlacesAndDayWithoutInventingMissingRows() {
        Long artSpaceId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM places
                        WHERE slug = 'demo-art-space'
                        """,
                        Long.class
                );

        Long cityGardenId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT id
                        FROM places
                        WHERE slug = 'demo-city-garden'
                        """,
                        Long.class
                );

        List<PlaceSchedulingOpeningHourRow> mondayRows =
                placeRepository.findSchedulingOpeningHourRows(
                        Set.of(
                                artSpaceId,
                                cityGardenId
                        ),
                        (short) 1
                );

        assertThat(mondayRows)
                .extracting(
                        PlaceSchedulingOpeningHourRow::getPlaceId
                )
                .containsExactly(
                        artSpaceId,
                        cityGardenId
                );

        PlaceSchedulingOpeningHourRow artSpaceMonday =
                findOpeningHourRow(
                        mondayRows,
                        artSpaceId
                );

        assertThat(artSpaceMonday.getClosed())
                .isFalse();

        assertThat(artSpaceMonday.getOpenTime())
                .isEqualTo(
                        LocalTime.of(9, 0)
                );

        assertThat(artSpaceMonday.getCloseTime())
                .isEqualTo(
                        LocalTime.of(17, 0)
                );
        List<PlaceSchedulingOpeningHourRow> tuesdayRows =
                placeRepository.findSchedulingOpeningHourRows(
                        Set.of(
                                artSpaceId,
                                cityGardenId
                        ),
                        (short) 2
                );

        PlaceSchedulingOpeningHourRow artSpaceTuesday =
                findOpeningHourRow(
                        tuesdayRows,
                        artSpaceId
                );

        assertThat(artSpaceTuesday.getClosed())
                .isTrue();

        assertThat(artSpaceTuesday.getOpenTime())
                .isNull();

        assertThat(artSpaceTuesday.getCloseTime())
                .isNull();
        List<PlaceSchedulingOpeningHourRow> sundayRows =
                placeRepository.findSchedulingOpeningHourRows(
                        Set.of(
                                artSpaceId,
                                cityGardenId
                        ),
                        (short) 7
                );

        assertThat(sundayRows)
                .extracting(
                        PlaceSchedulingOpeningHourRow::getPlaceId
                )
                .containsExactly(
                        artSpaceId
                );

        assertThat(sundayRows)
                .extracting(
                        PlaceSchedulingOpeningHourRow::getPlaceId
                )
                .doesNotContain(
                        cityGardenId
                );
    }
    private void assertInvalidPlace(
            String slug,
            BigDecimal latitude,
            BigDecimal longitude,
            int visitMinutes,
            BigDecimal minCost,
            BigDecimal maxCost,
            Boolean indoor
    ) {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        INSERT INTO places (
                            name, slug, address,
                            administrative_unit_name, administrative_unit_type,
                            latitude, longitude,
                            estimated_visit_minutes, min_cost, max_cost, indoor, active
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
                        """,
                "Constraint Test Place",
                slug,
                "Demo test address",
                "Đơn vị Demo",
                "WARD",
                latitude,
                longitude,
                visitMinutes,
                minCost,
                maxCost,
                indoor
        )).isInstanceOf(DataAccessException.class);
    }

    private void assertInvalidSql(String sql) {
        assertThatThrownBy(() -> jdbcTemplate.update(sql))
                .isInstanceOf(DataAccessException.class);
    }

    private void insertSearchPlace(
            String slug,
            String name,
            String shortDescription,
            String fullDescription,
            String address,
            String administrativeUnitName,
            boolean active
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO places (
                            name, slug, short_description, full_description, address,
                            administrative_unit_name, administrative_unit_type,
                            latitude, longitude, estimated_visit_minutes, min_cost, max_cost,
                            indoor, active
                        ) VALUES (?, ?, ?, ?, ?, ?, 'WARD', 10.7000000, 106.6000000, 60, 0, 100000, TRUE, ?)
                        """,
                name,
                slug,
                shortDescription,
                fullDescription,
                address,
                administrativeUnitName,
                active
        );
    }

    private void assignAdministrativeUnit(String name, String... slugs) {
        for (String slug : slugs) {
            jdbcTemplate.update(
                    "UPDATE places SET administrative_unit_name = ?, "
                            + "administrative_unit_type = 'WARD' WHERE slug = ?",
                    name,
                    slug
            );
        }
    }

    private PlaceSearchRequest searchRequest(
            String keyword,
            String administrativeUnitName,
            String category,
            Boolean indoor,
            BigDecimal maxCost,
            Integer page,
            Integer size
    ) {
        return new PlaceSearchRequest(
                keyword,
                administrativeUnitName,
                category,
                indoor,
                maxCost,
                page,
                size
        );
    }
    private PlaceSchedulingOpeningHourRow findOpeningHourRow(
            List<PlaceSchedulingOpeningHourRow> rows,
            Long placeId
    ) {
        return rows.stream()
                .filter(
                        row -> row
                                .getPlaceId()
                                .equals(placeId)
                )
                .findFirst()
                .orElseThrow(
                        () -> new AssertionError(
                                "Missing opening-hour row for placeId="
                                        + placeId
                        )
                );
    }
}
