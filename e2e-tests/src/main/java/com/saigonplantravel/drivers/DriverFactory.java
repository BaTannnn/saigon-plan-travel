package com.saigonplantravel.drivers;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class DriverFactory {
    public static WebDriver getDriver(String browser, boolean headless) {
        WebDriver driver = switch (browser.toLowerCase()) {
            case "firefox" -> createFirefox(headless);
            case "edge" -> createEdge(headless);
            case "chrome" -> createChrome(headless);
            default -> throw new IllegalArgumentException(
                "Unsupported browser: " + browser
        );
        };
        driver.manage().window().maximize();
        return driver;
    }
    private static WebDriver createChrome(boolean headless){
        ChromeOptions options = new ChromeOptions();
        if (headless){
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }
        return new ChromeDriver(options);
    }
    private static WebDriver createFirefox(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("-headless");
        }

        return new FirefoxDriver(options);
    }

    private static WebDriver createEdge(boolean headless) {
        EdgeOptions options = new EdgeOptions();

        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }

        return new EdgeDriver(options);
    }
}
