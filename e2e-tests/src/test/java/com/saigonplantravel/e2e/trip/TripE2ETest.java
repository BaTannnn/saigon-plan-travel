package com.saigonplantravel.e2e.trip;

import com.saigonplantravel.e2e.BaseE2ETest;
import com.saigonplantravel.e2e.pages.CreateTripPage;
import com.saigonplantravel.e2e.pages.HomePage;
import com.saigonplantravel.e2e.pages.LoginPage;
import com.saigonplantravel.e2e.pages.TripDetailPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripE2ETest extends BaseE2ETest {

    private static final String START_TIME = "08:00";
    private static final String END_TIME = "18:00";
    private static final String BUDGET = "500000";
    private static final String ORIGIN_QUERY =
            "Bưu điện Trung tâm Sài Gòn";

    private LoginPage loginPage;
    private HomePage homePage;
    private CreateTripPage createTripPage;
    private TripDetailPage tripDetailPage;

    @BeforeEach
    void createPages() {
        loginPage = new LoginPage(driver, wait, baseUrl);
        homePage = new HomePage(driver, wait, baseUrl);
        createTripPage = new CreateTripPage(driver, wait, baseUrl);
        tripDetailPage = new TripDetailPage(driver, wait, baseUrl);
    }

    @Test
    void shouldCreateTripSuccessfully() {
        LocalDate tripDate = LocalDate.now().plusDays(30);

        loginPage.open();
        loginPage.login(TEST_ACCOUNT_EMAIL, TEST_ACCOUNT_PASSWORD);
        homePage.waitForAuthenticatedState();

        createTripPage.open();
        createTripPage.waitUntilLoaded();
        createTripPage.setTripDate(tripDate);
        createTripPage.setBudget(BUDGET);
        String selectedOrigin =
                createTripPage.searchAndSelectFirstOrigin(ORIGIN_QUERY);
        createTripPage.submit();

        tripDetailPage.waitUntilLoaded();
        assertAll(
                () -> assertTrue(tripDetailPage.isCurrentTripDetailUrl()),
                () -> assertEquals(
                        selectedOrigin,
                        tripDetailPage.getDisplayedOrigin()
                ),
                () -> assertEquals(
                        START_TIME,
                        tripDetailPage.getDisplayedStartTime()
                ),
                () -> assertEquals(
                        END_TIME,
                        tripDetailPage.getDisplayedEndTime()
                ),
                () -> assertEquals(
                        "Cân bằng",
                        tripDetailPage.getDisplayedTravelPace()
                ),
                () -> assertEquals(
                        "Kết hợp",
                        tripDetailPage.getDisplayedEnvironmentPreference()
                )
        );
    }
}
