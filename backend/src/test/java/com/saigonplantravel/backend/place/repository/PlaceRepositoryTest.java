package com.saigonplantravel.backend.place.repository;

import com.saigonplantravel.backend.place.dto.PlaceDetailResponse;
import com.saigonplantravel.backend.place.entity.Category;
import com.saigonplantravel.backend.place.entity.Place;
import com.saigonplantravel.backend.place.service.PlaceService;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

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

    @Autowired
    private Flyway flyway;

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
    void appliesCanonicalMigrationsAndSeedExactlyOnce() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                String.class
        );

        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '6' AND success",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'vector'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM places", Integer.class))
                .isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM places WHERE active",
                Integer.class
        )).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM categories", Integer.class))
                .isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM place_categories",
                Integer.class
        )).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM opening_hours",
                Integer.class
        )).isEqualTo(33);

        MigrateResult rerun = flyway.migrate();

        assertThat(rerun.migrationsExecuted).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM places", Integer.class))
                .isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '6' AND success",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '8' AND success",
                Integer.class
        )).isEqualTo(1);
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
                            name, slug, address, district, latitude, longitude,
                            estimated_visit_minutes, min_cost, max_cost, indoor, active
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
                        """,
                "Constraint Test Place",
                slug,
                "Demo test address",
                "Quận 1",
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
}
