package com.saigonplantravel.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.regex.Pattern;

public class TripDetailPage {

    private static final By ORIGIN = By.cssSelector(
            "section[aria-labelledby='trip-origin-heading'] p"
    );
    private static final By START_TIME = By.xpath("(//article//time)[1]");
    private static final By END_TIME = By.xpath("(//article//time)[2]");
    private static final By TRIP_INFORMATION =
            By.cssSelector("section[aria-label='Thông tin chuyến đi']");
    private static final By TRAVEL_PACE = By.xpath(
            "//section[@aria-label='Thông tin chuyến đi']" +
                    "//div[p[normalize-space()='Nhịp độ']]/p[1]"
    );
    private static final By ENVIRONMENT_PREFERENCE = By.xpath(
            "//section[@aria-label='Thông tin chuyến đi']" +
                    "//div[p[normalize-space()='Không gian']]/p[1]"
    );

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String tripDetailUrlPattern;

    public TripDetailPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        this.driver = driver;
        this.wait = wait;
        this.tripDetailUrlPattern = Pattern.quote(baseUrl)
                + "/trips/[^/?#]+/?(?:[?#].*)?$";
    }

    public void waitUntilLoaded() {
        wait.until(ExpectedConditions.urlMatches(tripDetailUrlPattern));
        wait.until(ExpectedConditions.visibilityOfElementLocated(ORIGIN));
        wait.until(ExpectedConditions.visibilityOfElementLocated(START_TIME));
        wait.until(ExpectedConditions.visibilityOfElementLocated(END_TIME));
        wait.until(ExpectedConditions.visibilityOfElementLocated(TRIP_INFORMATION));
    }

    public boolean isCurrentTripDetailUrl() {
        return driver.getCurrentUrl().matches(tripDetailUrlPattern);
    }

    public String getDisplayedOrigin() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(ORIGIN))
                .getText();
    }

    public String getDisplayedStartTime() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(START_TIME))
                .getText();
    }

    public String getDisplayedEndTime() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(END_TIME))
                .getText();
    }

    public String getDisplayedTravelPace() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(TRAVEL_PACE))
                .getText();
    }

    public String getDisplayedEnvironmentPreference() {
        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(ENVIRONMENT_PREFERENCE)
        ).getText();
    }
}
