package com.saigonplantravel.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage {

    private static final By EMAIL_FIELD = By.id("login-email");
    private static final By PASSWORD_FIELD = By.id("login-password");
    private static final By LOGIN_BUTTON = By.cssSelector("button[type='submit']");
    private static final By ERROR_ALERT = By.cssSelector("[role='alert']");
    private static final By ERROR_MESSAGE =
            By.cssSelector("[role='alert'] [data-slot='alert-description']");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String loginUrl;

    public LoginPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        this.driver = driver;
        this.wait = wait;
        this.loginUrl = baseUrl + "/login";
    }

    public void open() {
        driver.get(loginUrl);
    }

    public void waitUntilFormIsVisible() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD));
        wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD));
        wait.until(ExpectedConditions.visibilityOfElementLocated(LOGIN_BUTTON));
    }

    public boolean isEmailFieldVisible() {
        return driver.findElement(EMAIL_FIELD).isDisplayed();
    }

    public boolean isPasswordFieldVisible() {
        return driver.findElement(PASSWORD_FIELD).isDisplayed();
    }

    public boolean isLoginButtonVisible() {
        return driver.findElement(LOGIN_BUTTON).isDisplayed();
    }

    public void enterEmail(String email) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD))
                .sendKeys(email);
    }

    public void enterPassword(String password) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD))
                .sendKeys(password);
    }

    public void clickLogin() {
        wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON)).click();
    }

    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLogin();
    }

    public String waitForLoginErrorMessage() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_ALERT));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_MESSAGE))
                .getText();
    }
}
