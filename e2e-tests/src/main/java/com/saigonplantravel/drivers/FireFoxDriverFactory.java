package com.saigonplantravel.drivers;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class FireFoxDriverFactory extends DriverFactory {

    @Override
    protected WebDriver getDriver() {
        return new FirefoxDriver();
    }
}
