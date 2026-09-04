package com.saigonplantravel.e2e.test;

import com.saigonplantravel.e2e.base.BaseTest;
import com.saigonplantravel.e2e.listeners.TestListener;
import com.saigonplantravel.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {
    @DataProvider(name = "loginData")
    public Object[][] loginData(){
        String email =
                System.getenv("E2E_EMAIL");

        String password =
                System.getenv("E2E_PASSWORD");
        return new Object[][] {
                {email, password}
        };
    }
    @Test(groups = "smoke", dataProvider = "loginData")
    public void loginTest(String email, String password) {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.login(email, password);
        loginPage.waitForLoginSuccess();
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:3000/");
    }
}
