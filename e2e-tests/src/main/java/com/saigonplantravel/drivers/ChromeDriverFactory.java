package com.saigonplantravel.drivers;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class ChromeDriverFactory extends DriverFactory {

    @Override
    protected WebDriver getDriver() {
        return new ChromeDriver();
    }
}
