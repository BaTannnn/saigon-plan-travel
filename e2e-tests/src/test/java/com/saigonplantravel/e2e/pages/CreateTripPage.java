package com.saigonplantravel.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CreateTripPage {

    private static final By TRIP_DATE_FIELD = By.id("trip-date");
    private static final By START_TIME_FIELD = By.id("start-time");
    private static final By END_TIME_FIELD = By.id("end-time");
    private static final By BUDGET_FIELD = By.id("trip-budget");
    private static final By ORIGIN_FIELD = By.id("origin-label");
    private static final By SEARCH_BUTTON =
            By.xpath("//form//button[@type='button' and normalize-space(.)='Tìm']");
    private static final By FIRST_ORIGIN_RESULT = By.cssSelector(
            "[aria-label='Kết quả tìm điểm xuất phát'] li:first-child button"
    );
    private static final By SUBMIT_BUTTON =
            By.cssSelector("form button[type='submit']");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String createTripUrl;

    public CreateTripPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        this.driver = driver;
        this.wait = wait;
        this.createTripUrl = baseUrl + "/trips/new";
    }

    public void open() {
        driver.get(createTripUrl);
    }

    public void waitUntilLoaded() {
        wait.until(ExpectedConditions.urlToBe(createTripUrl));
        wait.until(ExpectedConditions.visibilityOfElementLocated(TRIP_DATE_FIELD));
        wait.until(ExpectedConditions.visibilityOfElementLocated(ORIGIN_FIELD));
        wait.until(ExpectedConditions.visibilityOfElementLocated(SUBMIT_BUTTON));
    }

    public void setTripDate(LocalDate tripDate) {
        WebElement field = wait.until(
                ExpectedConditions.elementToBeClickable(TRIP_DATE_FIELD)
        );

        field.clear();

        String keyboardDate =
                tripDate.format(
                        DateTimeFormatter.ofPattern("MMddyyyy")
                );

        field.sendKeys(keyboardDate);

        wait.until(
                ExpectedConditions.attributeToBe(
                        TRIP_DATE_FIELD,
                        "value",
                        tripDate.toString()
                )
        );
    }

    public void setStartTime(String startTime) {
        setFieldValue(START_TIME_FIELD, startTime);
    }

    public void setEndTime(String endTime) {
        setFieldValue(END_TIME_FIELD, endTime);
    }

    public void setBudget(String budget) {
        setFieldValue(BUDGET_FIELD, budget);
    }

    public String searchAndSelectFirstOrigin(String query) {
        setFieldValue(ORIGIN_FIELD, query);
        wait.until(ExpectedConditions.elementToBeClickable(SEARCH_BUTTON)).click();

        WebElement firstResult = wait.until(
                ExpectedConditions.elementToBeClickable(FIRST_ORIGIN_RESULT)
        );
        String selectedLabel = firstResult.getText().trim();
        firstResult.click();

        wait.until(ExpectedConditions.attributeToBe(
                ORIGIN_FIELD,
                "value",
                selectedLabel
        ));
        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BUTTON));

        return selectedLabel;
    }

    public void submit() {
        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BUTTON)).click();
    }

    private void setFieldValue(By locator, String value) {
        WebElement field = wait.until(ExpectedConditions.elementToBeClickable(locator));
        field.clear();

        field.sendKeys(value);
        wait.until(ExpectedConditions.attributeToBe(locator, "value", value));
    }
}
