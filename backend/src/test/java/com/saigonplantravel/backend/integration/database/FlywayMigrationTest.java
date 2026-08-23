package com.saigonplantravel.backend.integration.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.saigonplantravel.backend.media.StoredMedia;
import com.saigonplantravel.backend.place.service.PlaceImageMetadataService;
import com.saigonplantravel.backend.testsupport.PostgresIntegrationTestSupport;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
class FlywayMigrationTest {

    @Container
    static final PostgreSQLContainer postgres = PostgresIntegrationTestSupport.newContainer();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresIntegrationTestSupport.registerCommonProperties(registry, postgres);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private PlaceImageMetadataService placeImageMetadataService;

    @Test
    void appliesCanonicalMigrationsAndSeedExactlyOnce() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM flyway_schema_history WHERE version = '6' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM places", Integer.class))
                .isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM places WHERE active", Integer.class))
                .isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM information_schema.columns "
                                + "WHERE table_schema = 'public' "
                                + "AND table_name = 'places' "
                                + "AND column_name IN ("
                                + "'administrative_unit_name', 'administrative_unit_type')",
                        Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM categories", Integer.class))
                .isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM place_categories", Integer.class))
                .isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM opening_hours", Integer.class))
                .isEqualTo(33);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM pg_extension WHERE extname = 'unaccent'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM pg_indexes "
                                + "WHERE schemaname = 'public' "
                                + "AND indexname = 'idx_place_categories_category_place'",
                        Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM trips", Integer.class))
                .isZero();

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM pg_tables WHERE schemaname = 'public' "
                                + "AND tablename = 'trip_category_preferences'",
                        Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                        """
                SELECT count(*)
                FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename IN (
                    'itineraries',
                    'itinerary_items'
                  )
                """,
                        Integer.class))
                .isEqualTo(2);

        MigrateResult rerun = flyway.migrate();

        assertThat(rerun.migrationsExecuted).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM places", Integer.class))
                .isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM flyway_schema_history WHERE version = '6' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM flyway_schema_history WHERE version = '8' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM flyway_schema_history WHERE version = '9' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM flyway_schema_history WHERE version = '10' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        """
                SELECT count(*)
                FROM flyway_schema_history
                WHERE version = '11'
                  AND success
                """,
                        Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM flyway_schema_history WHERE version = '13' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM flyway_schema_history WHERE version = '14' AND success", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM place_images", Integer.class))
                .isZero();
    }

    @Test
    void enforcesOneCoverAndCascadesMetadataWhenPlaceIsDeleted() {
        Long placeId = jdbcTemplate.queryForObject(
                """
                INSERT INTO places (
                    name, slug, address, latitude, longitude,
                    estimated_visit_minutes, min_cost, max_cost, indoor
                )
                VALUES ('Image test', 'image-test', 'Address', 10.77, 106.70, 60, 0, 0, FALSE)
                RETURNING id
                """,
                Long.class);
        jdbcTemplate.update(
                """
                INSERT INTO place_images (place_id, storage_key, url)
                VALUES (?, 'places/image-test/cover', 'https://cdn.example/cover.jpg')
                """,
                placeId);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO place_images (place_id, storage_key, url)
                        VALUES (?, 'places/image-test/second', 'https://cdn.example/second.jpg')
                        """,
                        placeId))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        jdbcTemplate.update("DELETE FROM places WHERE id = ?", placeId);

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM place_images WHERE place_id = ?", Integer.class, placeId))
                .isZero();
    }

    @Test
    void persistsReplacesAndRemovesCoverMetadata() {
        Long placeId = jdbcTemplate.queryForObject(
                """
                INSERT INTO places (
                    name, slug, address, latitude, longitude,
                    estimated_visit_minutes, min_cost, max_cost, indoor
                )
                VALUES ('Metadata test', 'metadata-test', 'Address', 10.77, 106.70, 60, 0, 0, FALSE)
                RETURNING id
                """,
                Long.class);

        String firstOldKey = placeImageMetadataService.replaceCover(
                "metadata-test", new StoredMedia("places/metadata/first", "https://cdn.example/first.jpg"));
        String replacedKey = placeImageMetadataService.replaceCover(
                "metadata-test", new StoredMedia("places/metadata/second", "https://cdn.example/second.jpg"));

        assertThat(firstOldKey).isNull();
        assertThat(replacedKey).isEqualTo("places/metadata/first");
        assertThat(jdbcTemplate.queryForList(
                        "SELECT storage_key FROM place_images WHERE place_id = ?", String.class, placeId))
                .containsExactly("places/metadata/second");

        String removedKey = placeImageMetadataService.removeCover("metadata-test");

        assertThat(removedKey).isEqualTo("places/metadata/second");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM place_images WHERE place_id = ?", Integer.class, placeId))
                .isZero();
        jdbcTemplate.update("DELETE FROM places WHERE id = ?", placeId);
    }

    @Test
    void appliesTripAndItineraryMigrationsAndCreatesFinalTables() {
        Integer migrationV7Count = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM flyway_schema_history
                        WHERE version = '7'
                          AND success
                        """,
                Integer.class);

        Integer migrationV8Count = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM flyway_schema_history
                        WHERE version = '8'
                          AND success
                        """,
                Integer.class);

        Integer migrationV9Count = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM flyway_schema_history
                        WHERE version = '9'
                          AND success
                        """,
                Integer.class);

        Integer migrationV10Count = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM flyway_schema_history
                        WHERE version = '10'
                          AND success
                        """,
                Integer.class);

        Integer migrationV13Count = jdbcTemplate.queryForObject(
                """
                        SELECT count(*)
                        FROM flyway_schema_history
                        WHERE version = '13'
                          AND success
                        """,
                Integer.class);

        Boolean tripsTableExists = jdbcTemplate.queryForObject(
                """
                        SELECT to_regclass(
                            'public.trips'
                        ) IS NOT NULL
                        """,
                Boolean.class);

        Boolean preferencesTableExists = jdbcTemplate.queryForObject(
                """
                        SELECT to_regclass(
                            'public.trip_category_preferences'
                        ) IS NOT NULL
                        """,
                Boolean.class);

        Boolean itinerariesTableExists =
                jdbcTemplate.queryForObject("SELECT to_regclass('public.itineraries') IS NOT NULL", Boolean.class);

        Boolean itineraryItemsTableExists =
                jdbcTemplate.queryForObject("SELECT to_regclass('public.itinerary_items') IS NOT NULL", Boolean.class);

        assertThat(migrationV7Count).isEqualTo(1);

        assertThat(migrationV8Count).isEqualTo(1);

        assertThat(migrationV9Count).isEqualTo(1);

        assertThat(migrationV10Count).isEqualTo(1);

        assertThat(migrationV13Count).isEqualTo(1);

        assertThat(tripsTableExists).isTrue();

        assertThat(preferencesTableExists).isFalse();

        assertThat(itinerariesTableExists).isTrue();

        assertThat(itineraryItemsTableExists).isTrue();
    }
}
