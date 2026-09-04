package com.saigonplantravel.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class BasePage {
    protected WebDriver driver;
    protected WebDriverWait wait;
    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(
                driver, Duration.ofSeconds(10)
        );
    }
    protected void waitForURL(String url) {
        wait.until(ExpectedConditions.urlToBe(url));
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }
    protected void type(By locator, String text) {
        WebElement element = waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

}
