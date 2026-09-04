package com.saigonplantravel.drivers;

import org.openqa.selenium.WebDriver;

public abstract class DriverFactory {
    protected abstract WebDriver getDriver();

    public WebDriver initializeDriver() {
        WebDriver driver = getDriver();

        driver.manage().window().maximize();
        driver.manage().deleteAllCookies();

        return driver;
    }
}
