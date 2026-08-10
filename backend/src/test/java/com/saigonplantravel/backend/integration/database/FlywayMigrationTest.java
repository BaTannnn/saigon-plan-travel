package com.saigonplantravel.backend.integration.database;

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
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class FlywayMigrationTest {

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
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void contextLoadsWithFlywayAndHibernateValidation() {
    }

    @Test
    void appliesCanonicalMigrationsAndSeedExactlyOnce() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                String.class
        );

        assertThat(versions).containsExactly(
                "1", "2", "3", "4", "5",
                "6", "7", "8", "9", "10",
                "11", "12", "13", "14", "15", "16"
        );
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
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM places "
                        + "WHERE administrative_unit_name IS NULL "
                        + "AND administrative_unit_type IS NULL",
                Integer.class
        )).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class))
                .isZero();
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
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'unaccent'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes "
                        + "WHERE schemaname = 'public' "
                        + "AND indexname = 'idx_place_categories_category_place'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM trips",
                Integer.class
        )).isZero();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM trip_category_preferences",
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename IN (
                    'itineraries',
                    'itinerary_preferred_categories',
                    'itinerary_items',
                    'itinerary_warnings'
                  )
                """,
                Integer.class
        )).isZero();

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
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '9' AND success",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '10' AND success",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM flyway_schema_history
                WHERE version = '11'
                  AND success
                """,
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM flyway_schema_history
                WHERE version = '16'
                  AND success
                """,
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM flyway_schema_history
                WHERE version = '12'
                  AND success
                """,
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void appliesTripMigrationsAndCreatesTables() {
        Integer migrationV11Count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT count(*)
                        FROM flyway_schema_history
                        WHERE version = '11'
                          AND success
                        """,
                        Integer.class
                );

        Integer migrationV12Count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT count(*)
                        FROM flyway_schema_history
                        WHERE version = '12'
                          AND success
                        """,
                        Integer.class
                );

        Boolean tripsTableExists =
                jdbcTemplate.queryForObject(
                        """
                        SELECT to_regclass(
                            'public.trips'
                        ) IS NOT NULL
                        """,
                        Boolean.class
                );

        Boolean preferencesTableExists =
                jdbcTemplate.queryForObject(
                        """
                        SELECT to_regclass(
                            'public.trip_category_preferences'
                        ) IS NOT NULL
                        """,
                        Boolean.class
                );

        assertThat(migrationV11Count)
                .isEqualTo(1);

        assertThat(migrationV12Count)
                .isEqualTo(1);

        assertThat(tripsTableExists)
                .isTrue();

        assertThat(preferencesTableExists)
                .isTrue();
    }
}
