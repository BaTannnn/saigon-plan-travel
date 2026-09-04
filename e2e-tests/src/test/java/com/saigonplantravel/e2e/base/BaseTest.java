package com.saigonplantravel.e2e.base;

import com.saigonplantravel.drivers.ChromeDriverFactory;
import com.saigonplantravel.drivers.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class BaseTest {
    protected WebDriver driver;
    private final DriverFactory driverFactory;
    protected BaseTest(DriverFactory driverFactory) {
        this.driverFactory = driverFactory;
    }
    @BeforeMethod
    public void setUp() {
        driver = driverFactory.initializeDriver();
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
