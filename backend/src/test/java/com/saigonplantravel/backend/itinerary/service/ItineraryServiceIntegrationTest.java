package com.saigonplantravel.backend.itinerary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.saigonplantravel.backend.itinerary.dto.ItineraryDetailResponse;
import com.saigonplantravel.backend.itinerary.exception.DuplicateItineraryPlaceException;
import com.saigonplantravel.backend.itinerary.exception.InactiveItineraryPlaceException;
import com.saigonplantravel.backend.itinerary.exception.InvalidItineraryOrderException;
import com.saigonplantravel.backend.itinerary.exception.ItineraryItemNotFoundException;
import com.saigonplantravel.backend.place.exception.PlaceNotFoundException;
import com.saigonplantravel.backend.testsupport.database.DatabaseTestFixtures;
import com.saigonplantravel.backend.testsupport.database.DatabaseTestFixtures.PlaceFixture;
import com.saigonplantravel.backend.testsupport.database.DatabaseTestFixtures.TripFixture;
import com.saigonplantravel.backend.trip.exception.TripNotFoundException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@Transactional
class ItineraryServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);

        registry.add("spring.datasource.username", postgres::getUsername);

        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.jpa.open-in-view", () -> "false");

        registry.add("app.security.jwt.secret", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }

    @Autowired
    private ItineraryService itineraryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private DatabaseTestFixtures fixtures;

    @BeforeEach
    void setUp() {

        fixtures = new DatabaseTestFixtures(jdbcTemplate);
    }

    @Test
    void getReturnsEmptyWithoutCreatingAnItineraryRow() {

        Long userId = fixtures.insertUser("empty-itinerary-owner");

        TripFixture trip = fixtures.insertValidTrip(userId);

        ItineraryDetailResponse response = itineraryService.getItinerary(userId, trip.tripPublicId());

        assertThat(response.publicId()).isNull();

        assertThat(response.tripPublicId()).isEqualTo(trip.tripPublicId());

        assertThat(response.items()).isEmpty();

        assertThat(response.summary()).isNotNull();

        assertThat(response.summary().totalEstimatedCost()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(response.summary().totalTravelMinutes()).isZero();

        assertThat(response.summary().totalVisitMinutes()).isZero();

        assertThat(response.summary().totalDistanceKm()).isZero();

        assertThat(response.issues()).isEmpty();

        assertThat(countRows("itineraries", "trip_id", trip.tripId())).isZero();
    }

    @Test
    void rejectsAccessToAnotherUsersTrip() {

        Long ownerId = fixtures.insertUser("itinerary-owner");

        Long anotherUserId = fixtures.insertUser("itinerary-stranger");

        TripFixture trip = fixtures.insertValidTrip(ownerId);

        assertThatThrownBy(() -> itineraryService.getItinerary(anotherUserId, trip.tripPublicId()))
                .isInstanceOf(TripNotFoundException.class);
    }

    @Test
    void firstAddCreatesItineraryAndLaterAddsAppendInOrder() {

        Long userId = fixtures.insertUser("append-owner");

        TripFixture trip = fixtures.insertValidTrip(userId);

        PlaceFixture first = fixtures.insertValidPlace("First place", "first-place", false);

        PlaceFixture second = fixtures.insertValidPlace("Second place", "second-place", true);

        ItineraryDetailResponse firstResponse = itineraryService.addItem(userId, trip.tripPublicId(), first.placeId());

        UUID itineraryPublicId = firstResponse.publicId();

        ItineraryDetailResponse secondResponse =
                itineraryService.addItem(userId, trip.tripPublicId(), second.placeId());

        ItineraryDetailResponse loadedResponse = itineraryService.getItinerary(userId, trip.tripPublicId());

        assertThat(secondResponse.publicId()).isEqualTo(itineraryPublicId);

        assertThat(secondResponse.items()).extracting(item -> item.sequenceNo()).containsExactly(1, 2);

        assertThat(secondResponse.items())
                .extracting(item -> item.place().slug())
                .containsExactly(placeSlug(first.placeId()), placeSlug(second.placeId()));

        assertThat(secondResponse.items()).allSatisfy(item -> {
            assertThat(item.schedule()).isNotNull();

            assertThat(item.schedule().arrivalTime()).isNotNull();

            assertThat(item.schedule().visitStartTime()).isNotNull();

            assertThat(item.schedule().visitEndTime()).isNotNull();
        });

        assertThat(secondResponse.summary()).isNotNull();

        assertThat(loadedResponse.publicId()).isEqualTo(itineraryPublicId);

        assertThat(loadedResponse.items())
                .extracting(item -> item.place().slug())
                .containsExactly(placeSlug(first.placeId()), placeSlug(second.placeId()));

        assertThat(countRows("itineraries", "trip_id", trip.tripId())).isEqualTo(1);

        assertThat(countRows("itinerary_items", "itinerary_id", itineraryId(trip.tripId())))
                .isEqualTo(2);
    }

    @Test
    void rejectsMissingInactiveAndDuplicatePlaces() {

        Long userId = fixtures.insertUser("validation-owner");

        TripFixture trip = fixtures.insertValidTrip(userId);

        PlaceFixture active = fixtures.insertValidPlace("Active place", "active-place", false);

        PlaceFixture inactive = fixtures.insertValidPlace("Inactive place", "inactive-place", true);

        jdbcTemplate.update(
                """
                UPDATE places
                SET active = FALSE
                WHERE id = ?
                """,
                inactive.placeId());

        assertThatThrownBy(() -> itineraryService.addItem(userId, trip.tripPublicId(), Long.MAX_VALUE))
                .isInstanceOf(PlaceNotFoundException.class);

        assertThatThrownBy(() -> itineraryService.addItem(userId, trip.tripPublicId(), inactive.placeId()))
                .isInstanceOf(InactiveItineraryPlaceException.class);

        itineraryService.addItem(userId, trip.tripPublicId(), active.placeId());

        assertThatThrownBy(() -> itineraryService.addItem(userId, trip.tripPublicId(), active.placeId()))
                .isInstanceOf(DuplicateItineraryPlaceException.class);
    }

    @Test
    void deleteResequencesRemainingItemsContinuously() {

        Long userId = fixtures.insertUser("delete-owner");

        TripFixture trip = fixtures.insertValidTrip(userId);

        PlaceFixture first = place("Delete first", "delete-first");

        PlaceFixture second = place("Delete second", "delete-second");

        PlaceFixture third = place("Delete third", "delete-third");

        itineraryService.addItem(userId, trip.tripPublicId(), first.placeId());

        ItineraryDetailResponse beforeDelete = itineraryService.addItem(userId, trip.tripPublicId(), second.placeId());

        UUID removedItemPublicId = beforeDelete.items().get(1).publicId();

        itineraryService.addItem(userId, trip.tripPublicId(), third.placeId());

        ItineraryDetailResponse response =
                itineraryService.deleteItem(userId, trip.tripPublicId(), removedItemPublicId);

        assertThat(response.items()).extracting(item -> item.sequenceNo()).containsExactly(1, 2);

        assertThat(response.items())
                .extracting(item -> item.place().slug())
                .containsExactly(placeSlug(first.placeId()), placeSlug(third.placeId()));

        /*
         * Item thứ hai phải có schedule mới sau khi
         * B bị xóa khỏi A -> B -> C.
         *
         * Recalculation lúc này phải chạy lại A -> C.
         */
        assertThat(response.items().get(1).schedule()).isNotNull();

        assertThat(response.items().get(1).schedule().arrivalTime()).isNotNull();

        entityManager.flush();

        assertThat(databaseSequences(trip.tripId())).containsExactly(1, 2);
    }

    @Test
    void deletingFinalItemKeepsEmptyItineraryRow() {

        Long userId = fixtures.insertUser("final-delete-owner");

        TripFixture trip = fixtures.insertValidTrip(userId);

        PlaceFixture place = place("Only place", "only-place");

        ItineraryDetailResponse created = itineraryService.addItem(userId, trip.tripPublicId(), place.placeId());

        ItineraryDetailResponse emptied = itineraryService.deleteItem(
                userId, trip.tripPublicId(), created.items().getFirst().publicId());

        assertThat(emptied.publicId()).isEqualTo(created.publicId());

        assertThat(emptied.items()).isEmpty();

        assertThat(emptied.summary().totalEstimatedCost()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(emptied.summary().totalTravelMinutes()).isZero();

        assertThat(emptied.summary().totalVisitMinutes()).isZero();

        assertThat(emptied.summary().totalDistanceKm()).isZero();

        assertThat(countRows("itineraries", "trip_id", trip.tripId())).isEqualTo(1);

        assertThat(countRows("itinerary_items", "itinerary_id", itineraryId(trip.tripId())))
                .isZero();
    }

    @Test
    void replaceKeepsItemIdentityAndSequence() {

        Long userId = fixtures.insertUser("replace-owner");

        TripFixture trip = fixtures.insertValidTrip(userId);

        PlaceFixture first = place("Replace first", "replace-first");

        PlaceFixture oldPlace = place("Replace old", "replace-old");

        PlaceFixture replacement = place("Replace new", "replace-new");

        itineraryService.addItem(userId, trip.tripPublicId(), first.placeId());

        ItineraryDetailResponse before = itineraryService.addItem(userId, trip.tripPublicId(), oldPlace.placeId());

        UUID itemPublicId = before.items().get(1).publicId();

        ItineraryDetailResponse after =
                itineraryService.replaceItemPlace(userId, trip.tripPublicId(), itemPublicId, replacement.placeId());

        assertThat(after.items().get(1).publicId()).isEqualTo(itemPublicId);

        assertThat(after.items().get(1).sequenceNo()).isEqualTo(2);

        assertThat(after.items().get(1).place().slug()).isEqualTo(placeSlug(replacement.placeId()));

        assertThat(after.items().get(1).schedule()).isNotNull();

        assertThat(after.items().get(1).schedule().arrivalTime()).isNotNull();
    }

    @Test
    void rejectsDuplicateReplacementAndItemFromAnotherItinerary() {

        Long firstUserId = fixtures.insertUser("scope-first-owner");

        Long secondUserId = fixtures.insertUser("scope-second-owner");

        TripFixture firstTrip = fixtures.insertValidTrip(firstUserId);

        TripFixture secondTrip = fixtures.insertValidTrip(secondUserId);

        PlaceFixture firstPlace = place("Scope first", "scope-first");

        PlaceFixture duplicatePlace = place("Scope duplicate", "scope-duplicate");

        PlaceFixture foreignPlace = place("Scope foreign", "scope-foreign");

        ItineraryDetailResponse firstResponse =
                itineraryService.addItem(firstUserId, firstTrip.tripPublicId(), firstPlace.placeId());

        itineraryService.addItem(firstUserId, firstTrip.tripPublicId(), duplicatePlace.placeId());

        ItineraryDetailResponse foreignResponse =
                itineraryService.addItem(secondUserId, secondTrip.tripPublicId(), foreignPlace.placeId());

        assertThatThrownBy(() -> itineraryService.replaceItemPlace(
                        firstUserId,
                        firstTrip.tripPublicId(),
                        firstResponse.items().getFirst().publicId(),
                        duplicatePlace.placeId()))
                .isInstanceOf(DuplicateItineraryPlaceException.class);

        assertThatThrownBy(() -> itineraryService.replaceItemPlace(
                        firstUserId,
                        firstTrip.tripPublicId(),
                        foreignResponse.items().getFirst().publicId(),
                        firstPlace.placeId()))
                .isInstanceOf(ItineraryItemNotFoundException.class);
    }

    @Test
    void reorderChangesItemOrderAndKeepsContinuousSequences() {

        Long userId = fixtures.insertUser("reorder-owner");

        TripFixture trip = fixtures.insertValidTrip(userId);

        PlaceFixture first = place("Reorder first", "reorder-first");

        PlaceFixture second = place("Reorder second", "reorder-second");

        PlaceFixture third = place("Reorder third", "reorder-third");

        itineraryService.addItem(userId, trip.tripPublicId(), first.placeId());

        itineraryService.addItem(userId, trip.tripPublicId(), second.placeId());

        ItineraryDetailResponse beforeReorder = itineraryService.addItem(userId, trip.tripPublicId(), third.placeId());

        UUID firstItemPublicId = beforeReorder.items().get(0).publicId();

        UUID secondItemPublicId = beforeReorder.items().get(1).publicId();

        UUID thirdItemPublicId = beforeReorder.items().get(2).publicId();

        ItineraryDetailResponse result = itineraryService.reorderItems(
                userId, trip.tripPublicId(), List.of(thirdItemPublicId, firstItemPublicId, secondItemPublicId));

        assertThat(result.items())
                .extracting(item -> item.publicId())
                .containsExactly(thirdItemPublicId, firstItemPublicId, secondItemPublicId);

        assertThat(result.items()).extracting(item -> item.sequenceNo()).containsExactly(1, 2, 3);

        assertThat(result.items())
                .extracting(item -> item.place().slug())
                .containsExactly(placeSlug(third.placeId()), placeSlug(first.placeId()), placeSlug(second.placeId()));

        /*
         * Recalculation phải chạy lại theo order mới.
         */
        assertThat(result.items()).allSatisfy(item -> {
            assertThat(item.schedule()).isNotNull();

            assertThat(item.schedule().arrivalTime()).isNotNull();

            assertThat(item.schedule().visitEndTime()).isNotNull();
        });

        entityManager.flush();

        assertThat(databaseSequences(trip.tripId())).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsReorderWhenItemsAreMissing() {

        Long userId = fixtures.insertUser("invalid-reorder-owner");

        TripFixture trip = fixtures.insertValidTrip(userId);

        PlaceFixture first = place("Invalid reorder first", "invalid-reorder-first");

        PlaceFixture second = place("Invalid reorder second", "invalid-reorder-second");

        ItineraryDetailResponse firstResponse = itineraryService.addItem(userId, trip.tripPublicId(), first.placeId());

        itineraryService.addItem(userId, trip.tripPublicId(), second.placeId());

        UUID firstItemPublicId = firstResponse.items().getFirst().publicId();

        assertThatThrownBy(() -> itineraryService.reorderItems(userId, trip.tripPublicId(), List.of(firstItemPublicId)))
                .isInstanceOf(InvalidItineraryOrderException.class);

        /*
         * Transaction rollback phải giữ DB
         * ở trạng thái hợp lệ.
         */
        entityManager.clear();

        ItineraryDetailResponse loaded = itineraryService.getItinerary(userId, trip.tripPublicId());

        assertThat(loaded.items()).hasSize(2);
    }

    private PlaceFixture place(String name, String slug) {

        return fixtures.insertValidPlace(name, slug, false);
    }

    private Long itineraryId(Long tripId) {

        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM itineraries
                WHERE trip_id = ?
                """,
                Long.class,
                tripId);
    }

    private Integer countRows(String table, String foreignKey, Long foreignKeyValue) {

        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + foreignKey + " = ?", Integer.class, foreignKeyValue);
    }

    private java.util.List<Integer> databaseSequences(Long tripId) {

        return jdbcTemplate.queryForList(
                """
                SELECT ii.sequence_no
                FROM itinerary_items ii
                JOIN itineraries i
                    ON i.id = ii.itinerary_id
                WHERE i.trip_id = ?
                ORDER BY ii.sequence_no
                """,
                Integer.class,
                tripId);
    }

    private String placeSlug(Long placeId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT slug
                FROM places
                WHERE id = ?
                """,
                String.class,
                placeId);
    }
}
