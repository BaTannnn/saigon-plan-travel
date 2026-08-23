package com.saigonplantravel.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.fail;

public abstract class BaseE2ETest {

    private static final String DEFAULT_BASE_URL = "http://localhost:3000";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String baseUrl;

    protected static final String email = requiredEnvironmentVariable("TEST_ACCOUNT_EMAIL");
    protected static final String password = requiredEnvironmentVariable("TEST_ACCOUNT_PASSWORD");

    protected static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            fail(name + " must be set to run authenticated E2E tests.");
        }

        return value;
    }
    @BeforeEach
    protected void setUpWebDriver() {
        String configuredBaseUrl = System.getenv("E2E_BASE_URL");
        baseUrl = configuredBaseUrl == null || configuredBaseUrl.isBlank()
                ? DEFAULT_BASE_URL
                : configuredBaseUrl.replaceAll("/+$", "");

        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, WAIT_TIMEOUT);
    }

    @AfterEach
    protected void closeWebDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected String url(String path) {
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }
}
