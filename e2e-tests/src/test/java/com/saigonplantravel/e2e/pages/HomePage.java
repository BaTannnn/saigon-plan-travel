package com.saigonplantravel.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class HomePage {

    private static final By LOGOUT_BUTTON =
            By.cssSelector("button[aria-label='Đăng xuất']");
    private static final By LOGIN_LINK = By.linkText("Đăng nhập");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String homeUrl;

    public HomePage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        this.driver = driver;
        this.wait = wait;
        this.homeUrl = baseUrl + "/";
    }

    public void waitForAuthenticatedState() {
        wait.until(ExpectedConditions.urlToBe(homeUrl));
        wait.until(ExpectedConditions.visibilityOfElementLocated(LOGOUT_BUTTON));
    }

    public boolean isAuthenticatedUiVisible() {
        return driver.findElement(LOGOUT_BUTTON).isDisplayed();
    }

    public void logout() {
        wait.until(ExpectedConditions.elementToBeClickable(LOGOUT_BUTTON)).click();
    }

    public void waitForGuestState() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(LOGIN_LINK));
    }

    public boolean isGuestUiVisible() {
        return driver.findElement(LOGIN_LINK).isDisplayed();
    }
}
