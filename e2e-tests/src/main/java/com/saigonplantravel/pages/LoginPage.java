package com.saigonplantravel.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {
    private final By emailField = By.id("login-email");
    private final By passwordField = By.id("login-password");
    private final By loginButton = By.xpath("//button[@type='submit']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void open(){
        driver.get("http://localhost:3000/login");
    }
    public void login(String email, String password) {
        type(emailField, email);
        type(passwordField, password);
        click(loginButton);
    }
    public void waitForLoginSuccess() {
        waitForURL("http://localhost:3000/");
    }
}
