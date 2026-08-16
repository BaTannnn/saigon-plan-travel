package com.saigonplantravel.backend.place.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saigonplantravel.backend.place.dto.OpeningHourResponse;
import com.saigonplantravel.backend.place.dto.OpeningHourState;
import com.saigonplantravel.backend.place.dto.PageResponse;
import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.PlaceSearchRequest;
import com.saigonplantravel.backend.place.dto.PlaceSummaryResponse;
import com.saigonplantravel.backend.place.dto.admin.AdminPlaceDetailResponse;
import com.saigonplantravel.backend.place.dto.admin.AdminPlaceSummaryResponse;
import com.saigonplantravel.backend.place.dto.admin.CategoryCreateRequest;
import com.saigonplantravel.backend.place.dto.admin.PlaceOpeningHoursRequest;
import com.saigonplantravel.backend.place.entity.Category;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.service.CategoryService;
import com.saigonplantravel.backend.place.service.PlaceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
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

@Testcontainers
@SpringBootTest
class PlaceRepositoryTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.security.jwt.secret", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PlaceService placeService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager entityManager;

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
        assertThat(finalPage.getContent()).extracting(Place::getName).containsExactly("Demo Science Center");

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
        assertInvalidPlace(
                "invalid-latitude",
                new BigDecimal("91"),
                new BigDecimal("106"),
                60,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true);
        assertInvalidPlace(
                "invalid-longitude",
                new BigDecimal("10"),
                new BigDecimal("181"),
                60,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true);
        assertInvalidPlace(
                "invalid-duration",
                new BigDecimal("10"),
                new BigDecimal("106"),
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true);
        assertInvalidPlace(
                "invalid-min-cost",
                new BigDecimal("10"),
                new BigDecimal("106"),
                60,
                new BigDecimal("-1"),
                BigDecimal.ZERO,
                true);
        assertInvalidPlace(
                "invalid-max-cost",
                new BigDecimal("10"),
                new BigDecimal("106"),
                60,
                new BigDecimal("100"),
                new BigDecimal("99"),
                true);
        assertInvalidPlace(
                "invalid-indoor",
                new BigDecimal("10"),
                new BigDecimal("106"),
                60,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null);
        assertInvalidPlace(
                "demo-art-space",
                new BigDecimal("10"),
                new BigDecimal("106"),
                60,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true);
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
    @Transactional
    void administrationCanListAndOpenInactivePlaceWhilePublicQueriesStillHideIt() {
        PlaceSearchRequest request = searchRequest(null, null, null, null, 0, 100);

        PageResponse<AdminPlaceSummaryResponse> adminPlaces = placeService.getPlacesForAdministration(0, 100);
        AdminPlaceDetailResponse inactivePlace =
                placeService.getPlaceDetailForAdministrationBySlug("demo-temporarily-hidden-place");
        PageResponse<PlaceSummaryResponse> publicPlaces = placeService.searchPlaces(request);

        assertThat(adminPlaces.content())
                .filteredOn(place -> !place.active())
                .extracting(place -> place.slug())
                .containsExactly("demo-temporarily-hidden-place");
        assertThat(inactivePlace.active()).isFalse();
        assertThat(publicPlaces.content())
                .extracting(place -> place.slug())
                .doesNotContain("demo-temporarily-hidden-place");
        assertThatThrownBy(() -> placeService.getPlaceDetailBySlug("demo-temporarily-hidden-place"))
                .hasMessage("Place not found");
    }

    @Test
    @Transactional
    void statusChangesImmediatelyControlPublicPlaceVisibility() {
        String slug = "demo-temporarily-hidden-place";

        placeService.activatePlace(slug);

        assertThat(placeService.getPlaceDetailBySlug(slug).slug()).isEqualTo(slug);
        assertThat(placeRepository.findBySlug(slug))
                .get()
                .extracting(Place::getActive)
                .isEqualTo(true);

        placeService.deactivatePlace(slug);

        assertThatThrownBy(() -> placeService.getPlaceDetailBySlug(slug))
                .isInstanceOf(com.saigonplantravel.backend.place.exception.PlaceNotFoundException.class)
                .hasMessage("Place not found");
        assertThat(placeRepository.findBySlug(slug))
                .get()
                .extracting(Place::getActive)
                .isEqualTo(false);
    }

    @Test
    @Transactional
    void categoryAssignmentChangesImmediatelyReachPublicPlaceDetail() {
        String slug = "demo-art-space";

        placeService.replacePlaceCategories(slug, List.of("khoa-hoc"));

        assertThat(placeService.getPlaceDetailBySlug(slug).categories())
                .extracting(category -> category.slug())
                .containsExactly("khoa-hoc");

        placeService.replacePlaceCategories(slug, List.of());

        assertThat(placeService.getPlaceDetailBySlug(slug).categories()).isEmpty();
    }

    @Test
    @Transactional
    void openingHourReplacementPersistsOpenClosedAndUnknownStates() {
        String slug = "demo-art-space";
        List<PlaceOpeningHoursRequest.Day> days = List.of(
                new PlaceOpeningHoursRequest.Day(
                        (short) 1, OpeningHourState.OPEN, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new PlaceOpeningHoursRequest.Day((short) 2, OpeningHourState.CLOSED, null, null),
                new PlaceOpeningHoursRequest.Day((short) 3, OpeningHourState.UNKNOWN, null, null),
                new PlaceOpeningHoursRequest.Day((short) 4, OpeningHourState.UNKNOWN, null, null),
                new PlaceOpeningHoursRequest.Day((short) 5, OpeningHourState.UNKNOWN, null, null),
                new PlaceOpeningHoursRequest.Day((short) 6, OpeningHourState.UNKNOWN, null, null),
                new PlaceOpeningHoursRequest.Day((short) 7, OpeningHourState.UNKNOWN, null, null));

        placeService.replacePlaceOpeningHours(slug, days);
        entityManager.flush();
        entityManager.clear();

        assertThat(placeService.getPlaceDetailBySlug(slug).openingHours())
                .containsExactly(
                        new OpeningHourResponse((short) 1, false, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                        new OpeningHourResponse((short) 2, true, null, null));
    }

    @Test
    void returnsAllCategoriesInStableOrder() {
        assertThat(categoryRepository.findAllByOrderByNameAscIdAsc())
                .extracting(Category::getName)
                .containsExactly("Khoa học", "Lịch sử", "Nghệ thuật", "Ngoài trời", "Văn hóa");
    }

    @Test
    @Transactional
    void createdCategoryReachesAdministrativeAndPublicCatalogs() {
        categoryService.createCategory(
                new CategoryCreateRequest("  Trải nghiệm  ", "  trai-nghiem  ", "  Trải nghiệm demo  "));
        entityManager.clear();

        assertThat(categoryService.getCategoriesForAdministration())
                .filteredOn(category -> category.slug().equals("trai-nghiem"))
                .singleElement()
                .satisfies(category -> {
                    assertThat(category.name()).isEqualTo("Trải nghiệm");
                    assertThat(category.description()).isEqualTo("Trải nghiệm demo");
                });
        assertThat(categoryService.getCategories())
                .filteredOn(category -> category.slug().equals("trai-nghiem"))
                .singleElement()
                .extracting(category -> category.name())
                .isEqualTo("Trải nghiệm");
    }

    @Test
    void rejectsInvalidCategoryAndOpeningHourStates() {
        assertInvalidSql(
                """
                INSERT INTO categories (name, slug)
                VALUES ('Invalid uppercase', 'Invalid-Slug')
                """);
        assertInvalidSql(
                """
                INSERT INTO categories (name, slug)
                VALUES ('Invalid double hyphen', 'invalid--slug')
                """);
        assertInvalidSql(
                """
                INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, closed)
                SELECT id, 1, TIME '09:00', TIME '17:00', TRUE
                FROM places WHERE slug = 'demo-temporarily-hidden-place'
                """);
        assertInvalidSql(
                """
                INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, closed)
                SELECT id, 2, TIME '09:00', NULL, FALSE
                FROM places WHERE slug = 'demo-temporarily-hidden-place'
                """);
        assertInvalidSql(
                """
                INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, closed)
                SELECT id, 3, TIME '17:00', TIME '09:00', FALSE
                FROM places WHERE slug = 'demo-temporarily-hidden-place'
                """);
        assertInvalidSql(
                """
                INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, closed)
                SELECT id, 8, TIME '09:00', TIME '17:00', FALSE
                FROM places WHERE slug = 'demo-temporarily-hidden-place'
                """);
        assertInvalidSql(
                """
                INSERT INTO place_categories (place_id, category_id)
                SELECT place.id, category.id
                FROM places place, categories category
                WHERE place.slug = 'demo-art-space'
                  AND category.slug = 'nghe-thuat'
                """);
        assertInvalidSql(
                """
                INSERT INTO opening_hours (place_id, day_of_week, open_time, close_time, closed)
                SELECT id, 1, TIME '09:00', TIME '17:00', FALSE
                FROM places WHERE slug = 'demo-art-space'
                """);
    }

    @Test
    void loadsDetailInAtMostThreeQueriesWithoutInventingUnknownDays() {
        Statistics statistics =
                entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
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
    void searchesKeywordAcrossSupportedTextFieldsWithoutCaseOrVietnameseDiacritics() {
        insertSearchPlace("search-name", "Bảo tàng Name", null, null, "Địa chỉ demo", true);
        insertSearchPlace(
                "search-short-description", "Search Short", "Không gian Bảo tàng", null, "Địa chỉ demo", true);
        insertSearchPlace("search-full-description", "Search Full", null, "Nội dung Bảo tàng", "Địa chỉ demo", true);
        insertSearchPlace("search-address", "Search Address", null, null, "Đường Bảo tàng", true);

        PageResponse<PlaceSummaryResponse> response =
                placeService.searchPlaces(searchRequest("BAO TANG", null, null, null, 0, 100));

        assertThat(response.content())
                .extracting(item -> item.slug())
                .containsExactlyInAnyOrder(
                        "search-name", "search-short-description", "search-full-description", "search-address");
    }

    @Test
    @Transactional
    void treatsLikeWildcardsAsLiteralCharacters() {
        insertSearchPlace("literal-wildcards", "Demo 50%_path\\name", null, null, "Địa chỉ demo", true);

        PageResponse<PlaceSummaryResponse> response =
                placeService.searchPlaces(searchRequest("50%_path\\name", null, null, null, 0, 100));

        assertThat(response.content()).extracting(item -> item.slug()).containsExactly("literal-wildcards");
    }

    @Test
    @Transactional
    void combinesAllFiltersAndExcludesInactivePlaces() {
        PageResponse<PlaceSummaryResponse> response =
                placeService.searchPlaces(searchRequest("demo", "van-hoa", true, new BigDecimal("100000"), 0, 100));

        assertThat(response.content())
                .extracting(item -> item.slug())
                .containsExactly("demo-art-space", "demo-history-hall")
                .doesNotContain("demo-temporarily-hidden-place");
        assertThat(response.totalElements()).isEqualTo(2);
    }

    @Test
    @Transactional
    void appliesCategoryIndoorAndBudgetSemanticsIndividually() {
        assertThat(placeService
                        .searchPlaces(searchRequest(null, "khong-ton-tai", null, null, 0, 100))
                        .content())
                .isEmpty();

        assertThat(placeService
                        .searchPlaces(searchRequest(null, null, false, null, 0, 100))
                        .content())
                .extracting(item -> item.slug())
                .containsExactly("demo-city-garden", "demo-riverside-walk");

        assertThat(placeService
                        .searchPlaces(searchRequest(null, null, null, BigDecimal.ZERO, 0, 100))
                        .content())
                .extracting(item -> item.slug())
                .containsExactly("demo-city-garden", "demo-riverside-walk");
    }

    @Test
    void usesAtMostContentAndCountQueriesForCategoryPagination() {
        Statistics statistics =
                entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        PageResponse<PlaceSummaryResponse> response =
                placeService.searchPlaces(searchRequest(null, "van-hoa", null, null, 0, 1));

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
                    true);
        }

        PlaceSearchRequest firstPageRequest = searchRequest("fixture search target", null, null, null, 0, 10);
        PlaceSearchRequest secondPageRequest = searchRequest("fixture search target", null, null, null, 1, 10);
        Statistics statistics =
                entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        statistics.clear();
        long firstPageStartedAt = System.nanoTime();
        PageResponse<PlaceSummaryResponse> firstPage = placeService.searchPlaces(firstPageRequest);
        long firstPageElapsedNanos = System.nanoTime() - firstPageStartedAt;
        assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 2L);

        statistics.clear();
        long secondPageStartedAt = System.nanoTime();
        PageResponse<PlaceSummaryResponse> secondPage = placeService.searchPlaces(secondPageRequest);
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
                        secondPage.content().stream().map(item -> item.id()).toList());
        assertThat(secondPage.content()).extracting(item -> item.id()).isSorted();
        assertThat(firstPageElapsedNanos).isLessThan(500_000_000L);
        assertThat(secondPageElapsedNanos).isLessThan(500_000_000L);
    }

    private void assertInvalidPlace(
            String slug,
            BigDecimal latitude,
            BigDecimal longitude,
            int visitMinutes,
            BigDecimal minCost,
            BigDecimal maxCost,
            Boolean indoor) {
        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO places (
                            name, slug, address,
                            latitude, longitude,
                            estimated_visit_minutes, min_cost, max_cost, indoor, active
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
                        """,
                        "Constraint Test Place",
                        slug,
                        "Demo test address",
                        latitude,
                        longitude,
                        visitMinutes,
                        minCost,
                        maxCost,
                        indoor))
                .isInstanceOf(DataAccessException.class);
    }

    private void assertInvalidSql(String sql) {
        assertThatThrownBy(() -> jdbcTemplate.update(sql)).isInstanceOf(DataAccessException.class);
    }

    private void insertSearchPlace(
            String slug, String name, String shortDescription, String fullDescription, String address, boolean active) {
        jdbcTemplate.update(
                """
                        INSERT INTO places (
                            name, slug, short_description, full_description, address,
                            latitude, longitude, estimated_visit_minutes, min_cost, max_cost,
                            indoor, active
                        ) VALUES (?, ?, ?, ?, ?, 10.7000000, 106.6000000, 60, 0, 100000, TRUE, ?)
                        """,
                name,
                slug,
                shortDescription,
                fullDescription,
                address,
                active);
    }

    private PlaceSearchRequest searchRequest(
            String keyword, String category, Boolean indoor, BigDecimal maxCost, Integer page, Integer size) {
        return new PlaceSearchRequest(keyword, category, indoor, maxCost, page, size);
    }
}
