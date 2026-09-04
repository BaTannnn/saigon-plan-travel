package com.saigonplantravel.e2e.listeners;

import com.saigonplantravel.e2e.base.BaseTest;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestListener implements ITestListener {
    @Override
    public void onTestFailure(ITestResult result) {
        Object testInstance =  result.getInstance();

        if (!(testInstance instanceof BaseTest)) {
            return;
        }
        BaseTest baseTest = (BaseTest) result.getInstance();
        WebDriver driver = baseTest.getDriver();

        if (driver == null) {
            return;
        }

        takeScreenshot(driver, result.getName());
    }
    public void takeScreenshot(WebDriver driver, String testName) {
        TakesScreenshot screenshot = (TakesScreenshot) driver;
        File source =  screenshot.getScreenshotAs(OutputType.FILE);
        String timestamp =
                LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern(
                                "yyyyMMdd_HHmmss"
                        ));

        Path destination = Path.of("target", "screenshots", testName + "_" + timestamp + ".png");
        try {
            Files.copy(
                    source.toPath(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println(
                    "Screenshot saved: "
                            + destination.toAbsolutePath()
            );
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
