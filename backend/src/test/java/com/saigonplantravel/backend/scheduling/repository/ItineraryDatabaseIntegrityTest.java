package com.saigonplantravel.backend.scheduling.repository;

import com.saigonplantravel.backend.testsupport.database.DatabaseTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import com.saigonplantravel.backend.testsupport.database
        .DatabaseTestFixtures.PlaceFixture;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class ItineraryDatabaseIntegrityTest {
    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    DockerImageName
                            .parse("pgvector/pgvector:pg16")
                            .asCompatibleSubstituteFor(
                                    "postgres"
                            )
            );

    @DynamicPropertySource
    static void databaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );

        registry.add(
                "spring.jpa.open-in-view",
                () -> "false"
        );

        registry.add(
                "app.security.jwt.secret",
                () ->
                        "MDEyMzQ1Njc4OWFiY2RlZjAx"
                                + "MjM0NTY3ODlhYmNkZWY="
        );
    }
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DatabaseTestFixtures fixtures;
    @BeforeEach
    void setUpFixtures() {
        fixtures =
                new DatabaseTestFixtures(
                        jdbcTemplate
                );
    }
    // Test và helper ở các phần bên dưới
    private record Fixture(
            Long userId,
            Long tripId,
            UUID tripPublicId,
            Long categoryId
    ) {
    }
    @Test
    void rejectsInvalidItinerarySummaryRows() {
        Fixture fixture = createFixture();

        Long itineraryId =
                insertValidItinerary(
                        UUID.randomUUID(),
                        fixture
                );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET window_end = window_start
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET origin_latitude = 91.0000000
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET origin_longitude = 181.0000000
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET travel_pace = 'SLOW'
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET environment_preference = 'ANY'
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET total_estimated_cost = 600000.00
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET remaining_budget = 1.00
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET total_distance_km = -1.000
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET total_travel_minutes = -1
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET scheduled_count = 0
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET scheduled_count = candidate_count + 1
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET algorithm_version = 'GREEDY_V2'
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET average_speed_kmh = 0.00
                WHERE id = ?
                """,
                itineraryId
        );

        assertInvalidUpdate(
                """
                UPDATE itineraries
                SET fixed_transfer_minutes = -1
                WHERE id = ?
                """,
                itineraryId
        );
    }
    @Test
    void rejectsDuplicateItineraryPublicId() {
        Fixture fixture = createFixture();

        UUID publicId =
                UUID.randomUUID();

        insertValidItinerary(
                publicId,
                fixture
        );

        assertThatThrownBy(
                () -> insertValidItinerary(
                        publicId,
                        fixture
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }
    @Test
    void createsOwnershipLookupIndex() {
        String indexDefinition =
                jdbcTemplate.queryForObject(
                        """
                        SELECT indexdef
                        FROM pg_indexes
                        WHERE schemaname = current_schema()
                          AND tablename = 'itineraries'
                          AND indexname =
                              'idx_itineraries_user_public_id'
                        """,
                        String.class
                );

        assertThat(indexDefinition)
                .isNotNull()
                .contains(
                        "(user_id, public_id)"
                );
    }
    @Test
    void declaresExpectedForeignKeyDeleteRules() {
        Map<String, String> deleteRules =
                jdbcTemplate.query(
                        """
                        SELECT
                            constraint_name,
                            delete_rule
                        FROM information_schema
                            .referential_constraints
                        WHERE constraint_schema =
                                current_schema()
                          AND constraint_name IN (
                              'fk_itineraries_user',
                              'fk_itineraries_trip',
                              'fk_itinerary_preferred_categories_itinerary',
                              'fk_itinerary_preferred_categories_category',
                              'fk_itinerary_items_itinerary',
                              'fk_itinerary_items_place',
                              'fk_itinerary_warnings_itinerary'
                          )
                        """,
                        resultSet -> {
                            Map<String, String> rules =
                                    new HashMap<>();

                            while (resultSet.next()) {
                                rules.put(
                                        resultSet.getString(
                                                "constraint_name"
                                        ),
                                        resultSet.getString(
                                                "delete_rule"
                                        )
                                );
                            }

                            return rules;
                        }
                );

        assertThat(deleteRules)
                .containsEntry(
                        "fk_itineraries_user",
                        "RESTRICT"
                )
                .containsEntry(
                        "fk_itineraries_trip",
                        "RESTRICT"
                )
                .containsEntry(
                        "fk_itinerary_preferred_categories_itinerary",
                        "CASCADE"
                )
                .containsEntry(
                        "fk_itinerary_preferred_categories_category",
                        "RESTRICT"
                );
        assertThat(deleteRules)
                .containsEntry(
                        "fk_itinerary_items_itinerary",
                        "CASCADE"
                )
                .containsEntry(
                        "fk_itinerary_items_place",
                        "RESTRICT"
                )
                .containsEntry(
                        "fk_itinerary_warnings_itinerary",
                        "CASCADE"
                );
    }
    @Test
    void enforcesCategorySnapshotIntegrityAndDeleteBehavior() {
        Fixture fixture = createFixture();

        Long itineraryId =
                insertValidItinerary(
                        UUID.randomUUID(),
                        fixture
                );

        jdbcTemplate.update(
                """
                INSERT INTO itinerary_preferred_categories (
                    itinerary_id,
                    category_id
                )
                VALUES (?, ?)
                """,
                itineraryId,
                fixture.categoryId()
        );

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO itinerary_preferred_categories (
                            itinerary_id,
                            category_id
                        )
                        VALUES (?, ?)
                        """,
                        itineraryId,
                        fixture.categoryId()
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        DELETE FROM categories
                        WHERE id = ?
                        """,
                        fixture.categoryId()
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        DELETE FROM trips
                        WHERE id = ?
                        """,
                        fixture.tripId()
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );

        jdbcTemplate.update(
                """
                DELETE FROM itineraries
                WHERE id = ?
                """,
                itineraryId
        );

        Integer snapshotCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT count(*)
                        FROM itinerary_preferred_categories
                        WHERE itinerary_id = ?
                        """,
                        Integer.class,
                        itineraryId
                );

        assertThat(snapshotCount)
                .isZero();
    }
    @Test
    void rejectsInvalidItineraryItemRows() {
        Fixture fixture =
                createFixture();

        Long itineraryId =
                insertValidItinerary(
                        UUID.randomUUID(),
                        fixture
                );

        PlaceFixture place =
                fixtures.insertValidPlace(
                        "Item Constraint Place",
                        "item-constraint-place",
                        false
                );

        Long itemId =
                insertKnownOpenItem(
                        itineraryId,
                        1,
                        place
                );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET sequence_no = 0
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET place_name = '   '
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET place_administrative_unit_type =
                    'DISTRICT'
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET latitude = 91.0000000
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET longitude = 181.0000000
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET base_visit_minutes = 0
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET travel_minutes_from_previous = -1
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET waiting_minutes = -1
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET visit_minutes = 0
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET distance_km_from_previous = -1.000
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET estimated_cost = -1.00
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET selection_score = 100.0001
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET arrival_time = TIME '09:00',
                    visit_start = TIME '08:30'
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET visit_end = visit_start
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET opening_time = NULL
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET closing_time = TIME '09:00'
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET opening_hours_status = 'UNKNOWN'
                WHERE id = ?
                """,
                itemId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_items
                SET opening_hours_status = 'CLOSED'
                WHERE id = ?
                """,
                itemId
        );
    }
    @Test
    void acceptsUnknownOpeningHoursWithoutArtificialInterval() {
        Fixture fixture =
                createFixture();

        Long itineraryId =
                insertValidItinerary(
                        UUID.randomUUID(),
                        fixture
                );

        PlaceFixture place =
                fixtures.insertValidPlace(
                        "Unknown Hours Place",
                        "unknown-hours-place",
                        true
                );

        Long itemId =
                insertUnknownHoursItem(
                        itineraryId,
                        1,
                        place
                );

        Map<String, Object> storedItem =
                jdbcTemplate.queryForMap(
                        """
                        SELECT
                            opening_hours_status,
                            opening_time,
                            closing_time
                        FROM itinerary_items
                        WHERE id = ?
                        """,
                        itemId
                );

        assertThat(
                storedItem.get(
                        "opening_hours_status"
                )
        ).isEqualTo("UNKNOWN");

        assertThat(
                storedItem.get("opening_time")
        ).isNull();

        assertThat(
                storedItem.get("closing_time")
        ).isNull();
    }
    @Test
    void rejectsDuplicateItemSequenceAndPlace() {
        Fixture fixture =
                createFixture();

        Long itineraryId =
                insertValidItinerary(
                        UUID.randomUUID(),
                        fixture
                );

        PlaceFixture firstPlace =
                fixtures.insertValidPlace(
                        "First Item Place",
                        "first-item-place",
                        false
                );

        PlaceFixture secondPlace =
                fixtures.insertValidPlace(
                        "Second Item Place",
                        "second-item-place",
                        true
                );

        insertKnownOpenItem(
                itineraryId,
                1,
                firstPlace
        );

        assertThatThrownBy(
                () -> insertKnownOpenItem(
                        itineraryId,
                        1,
                        secondPlace
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );

        assertThatThrownBy(
                () -> insertKnownOpenItem(
                        itineraryId,
                        2,
                        firstPlace
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }
    @Test
    void rejectsInvalidItineraryWarningRows() {
        Fixture fixture =
                createFixture();

        Long itineraryId =
                insertValidItinerary(
                        UUID.randomUUID(),
                        fixture
                );

        PlaceFixture place =
                fixtures.insertValidPlace(
                        "Warning Place",
                        "warning-place",
                        false
                );

        insertUnknownHoursItem(
                itineraryId,
                1,
                place
        );

        Long itineraryWarningId =
                insertWarning(
                        itineraryId,
                        null,
                        "TRAVEL_TIME_ESTIMATED",
                        "Thời gian di chuyển chỉ là ước tính.",
                        0
                );

        Long itemWarningId =
                insertWarning(
                        itineraryId,
                        1,
                        "OPENING_HOURS_UNKNOWN",
                        "Chưa có dữ liệu giờ mở cửa.",
                        1
                );

        assertInvalidUpdate(
                """
                UPDATE itinerary_warnings
                SET item_sequence = 0
                WHERE id = ?
                """,
                itemWarningId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_warnings
                SET sort_order = -1
                WHERE id = ?
                """,
                itineraryWarningId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_warnings
                SET code = 'UNKNOWN_WARNING'
                WHERE id = ?
                """,
                itineraryWarningId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_warnings
                SET message = '   '
                WHERE id = ?
                """,
                itineraryWarningId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_warnings
                SET item_sequence = NULL
                WHERE id = ?
                """,
                itemWarningId
        );

        assertInvalidUpdate(
                """
                UPDATE itinerary_warnings
                SET item_sequence = 1
                WHERE id = ?
                """,
                itineraryWarningId
        );

        assertThatThrownBy(
                () -> insertWarning(
                        itineraryId,
                        null,
                        "COST_USES_MINIMUM_ESTIMATE",
                        "Chi phí dùng mức tối thiểu.",
                        0
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }
    @Test
    void enforcesItemAndWarningDeleteBehavior() {
        Fixture fixture =
                createFixture();

        Long itineraryId =
                insertValidItinerary(
                        UUID.randomUUID(),
                        fixture
                );

        PlaceFixture place =
                fixtures.insertValidPlace(
                        "Delete Behavior Place",
                        "delete-behavior-place",
                        false
                );

        insertKnownOpenItem(
                itineraryId,
                1,
                place
        );

        insertWarning(
                itineraryId,
                null,
                "TRAVEL_TIME_ESTIMATED",
                "Thời gian di chuyển chỉ là ước tính.",
                0
        );

        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        """
                        DELETE FROM places
                        WHERE id = ?
                        """,
                        place.placeId()
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );

        jdbcTemplate.update(
                """
                DELETE FROM itineraries
                WHERE id = ?
                """,
                itineraryId
        );

        Integer itemCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT count(*)
                        FROM itinerary_items
                        WHERE itinerary_id = ?
                        """,
                        Integer.class,
                        itineraryId
                );

        Integer warningCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT count(*)
                        FROM itinerary_warnings
                        WHERE itinerary_id = ?
                        """,
                        Integer.class,
                        itineraryId
                );

        assertThat(itemCount)
                .isZero();

        assertThat(warningCount)
                .isZero();
    }
    //Helper
    private Fixture createFixture() {
        Long userId = fixtures.insertUser(
                "itinerary-integrity"
        );

        UUID tripPublicId =
                UUID.randomUUID();

        Long tripId = fixtures.insertValidTrip(
                tripPublicId,
                userId
        );

        Long categoryId = fixtures.insertCategory(
                "Itinerary Integrity Category",
                "itinerary-integrity-category"
        );

        return new Fixture(
                userId,
                tripId,
                tripPublicId,
                categoryId
        );
    }

    private Long insertValidItinerary(
            UUID itineraryPublicId,
            Fixture fixture
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO itineraries (
                    public_id,
                    user_id,
                    trip_id,
                    trip_public_id_snapshot,
                    trip_updated_at_snapshot,
    
                    trip_date,
                    window_start,
                    window_end,
    
                    origin_latitude,
                    origin_longitude,
    
                    travel_pace,
                    environment_preference,
    
                    initial_budget,
                    total_estimated_cost,
                    remaining_budget,
    
                    total_distance_km,
                    total_travel_minutes,
                    total_visit_minutes,
                    total_waiting_minutes,
                    remaining_minutes,
    
                    candidate_count,
                    scheduled_count,
    
                    algorithm_version,
                    average_speed_kmh,
                    fixed_transfer_minutes,
    
                    generated_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    TIMESTAMPTZ '2099-08-01 10:00:00+07',
    
                    DATE '2099-08-20',
                    TIME '08:00',
                    TIME '18:00',
    
                    10.7726400,
                    106.6980500,
    
                    'BALANCED',
                    'MIXED',
    
                    500000.00,
                    100000.00,
                    400000.00,
    
                    5.250,
                    30,
                    180,
                    0,
                    390,
    
                    5,
                    2,
    
                    'GREEDY_V1',
                    18.00,
                    5,
    
                    TIMESTAMPTZ '2099-08-01 11:00:00+07'
                )
                RETURNING id
                """,
                Long.class,
                itineraryPublicId,
                fixture.userId(),
                fixture.tripId(),
                fixture.tripPublicId()
        );
    }
    private void assertInvalidUpdate(
            String sql,
            Object... arguments
    ) {
        assertThatThrownBy(
                () -> jdbcTemplate.update(
                        sql,
                        arguments
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }
    private Long insertKnownOpenItem(
            Long itineraryId,
            int sequenceNo,
            PlaceFixture place
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO itinerary_items (
                    itinerary_id,
                    sequence_no,
    
                    place_id,
                    place_name,
                    place_slug,
                    place_address,
                    place_administrative_unit_name,
                    place_administrative_unit_type,
    
                    latitude,
                    longitude,
                    indoor,
    
                    base_visit_minutes,
    
                    travel_minutes_from_previous,
                    distance_km_from_previous,
    
                    arrival_time,
                    waiting_minutes,
                    visit_start,
                    visit_end,
                    visit_minutes,
    
                    estimated_cost,
                    selection_score,
    
                    opening_hours_status,
                    opening_time,
                    closing_time
                )
                VALUES (
                    ?, ?,
    
                    ?, ?, ?, ?, ?, ?,
    
                    ?, ?, ?,
    
                    ?,
    
                    10,
                    1.250,
    
                    TIME '08:15',
                    0,
                    TIME '08:15',
                    TIME '09:45',
                    90,
    
                    ?,
                    85.2500,
    
                    'KNOWN_OPEN',
                    TIME '08:00',
                    TIME '18:00'
                )
                RETURNING id
                """,
                Long.class,
                itineraryId,
                sequenceNo,

                place.placeId(),
                place.name(),
                place.slug(),
                place.address(),
                place.administrativeUnitName(),
                place.administrativeUnitType(),

                place.latitude(),
                place.longitude(),
                place.indoor(),

                place.estimatedVisitMinutes(),
                place.minCost()
        );
    }
    private Long insertUnknownHoursItem(
            Long itineraryId,
            int sequenceNo,
            PlaceFixture place
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO itinerary_items (
                    itinerary_id,
                    sequence_no,
    
                    place_id,
                    place_name,
                    place_slug,
                    place_address,
                    place_administrative_unit_name,
                    place_administrative_unit_type,
    
                    latitude,
                    longitude,
                    indoor,
    
                    base_visit_minutes,
    
                    travel_minutes_from_previous,
                    distance_km_from_previous,
    
                    arrival_time,
                    waiting_minutes,
                    visit_start,
                    visit_end,
                    visit_minutes,
    
                    estimated_cost,
                    selection_score,
    
                    opening_hours_status,
                    opening_time,
                    closing_time
                )
                VALUES (
                    ?, ?,
    
                    ?, ?, ?, ?, ?, ?,
    
                    ?, ?, ?,
    
                    ?,
    
                    15,
                    2.100,
    
                    TIME '10:00',
                    0,
                    TIME '10:00',
                    TIME '11:30',
                    90,
    
                    ?,
                    72.5000,
    
                    'UNKNOWN',
                    NULL,
                    NULL
                )
                RETURNING id
                """,
                Long.class,
                itineraryId,
                sequenceNo,

                place.placeId(),
                place.name(),
                place.slug(),
                place.address(),
                place.administrativeUnitName(),
                place.administrativeUnitType(),

                place.latitude(),
                place.longitude(),
                place.indoor(),

                place.estimatedVisitMinutes(),
                place.minCost()
        );
    }
    private Long insertWarning(
            Long itineraryId,
            Integer itemSequence,
            String code,
            String message,
            int sortOrder
    ) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO itinerary_warnings (
                    itinerary_id,
                    item_sequence,
                    code,
                    message,
                    sort_order
                )
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                itineraryId,
                itemSequence,
                code,
                message,
                sortOrder
        );
    }
}