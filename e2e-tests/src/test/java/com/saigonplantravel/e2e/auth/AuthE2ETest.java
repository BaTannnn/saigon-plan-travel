package com.saigonplantravel.e2e.auth;

import com.saigonplantravel.e2e.BaseE2ETest;
import com.saigonplantravel.e2e.pages.HomePage;
import com.saigonplantravel.e2e.pages.LoginPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class AuthE2ETest extends BaseE2ETest {

    private static final String INVALID_EMAIL = "invalid-test@example.com";
    private static final String INVALID_PASSWORD = "wrong-password";
    private static final String INVALID_CREDENTIALS_MESSAGE =
            "Email hoặc mật khẩu không đúng.";

    private LoginPage loginPage;
    private HomePage homePage;

    @BeforeEach
    void createPages() {
        loginPage = new LoginPage(driver, wait, baseUrl);
        homePage = new HomePage(driver, wait, baseUrl);
    }

    @Test
    void shouldDisplayLoginForm() {
        loginPage.open();
        loginPage.waitUntilFormIsVisible();

        assertAll(
                () -> assertTrue(loginPage.isEmailFieldVisible()),
                () -> assertTrue(loginPage.isPasswordFieldVisible()),
                () -> assertTrue(loginPage.isLoginButtonVisible())
        );
    }

    @Test
    void shouldShowErrorWhenCredentialsAreInvalid() {
        loginPage.open();
        loginPage.login(INVALID_EMAIL, INVALID_PASSWORD);

        assertEquals(
                INVALID_CREDENTIALS_MESSAGE,
                loginPage.waitForLoginErrorMessage()
        );
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
        loginPage.open();
        loginPage.login(email, password);

        homePage.waitForAuthenticatedState();
        assertTrue(homePage.isAuthenticatedUiVisible());
    }

    @Test
    void shouldLogoutSuccessfullyAndLoseProtectedAccess() {
        loginPage.open();
        loginPage.login(email, password);
        homePage.waitForAuthenticatedState();

        homePage.logout();
        homePage.waitForGuestState();
        assertTrue(homePage.isGuestUiVisible());

        driver.get(url("/trips"));
        wait.until(ExpectedConditions.urlToBe(url("/login")));
        assertEquals(url("/login"), driver.getCurrentUrl());
    }


}
