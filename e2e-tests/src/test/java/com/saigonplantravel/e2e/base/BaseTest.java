package com.saigonplantravel.e2e.base;

import com.saigonplantravel.drivers.Browser;
import com.saigonplantravel.drivers.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class BaseTest {
    protected WebDriver driver;
    @BeforeMethod
    public void setUp() {
        String browser = System.getProperty("browser", "chrome");

        boolean headless =
                Boolean.parseBoolean(System.getProperty("headless", "false"));
        driver = DriverFactory.getDriver(browser, headless);
    }
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    public WebDriver getDriver() {
        return driver;
    }
}
