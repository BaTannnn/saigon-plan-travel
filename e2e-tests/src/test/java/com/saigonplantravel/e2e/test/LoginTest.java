package com.saigonplantravel.e2e.test;

import com.saigonplantravel.drivers.ChromeDriverFactory;
import com.saigonplantravel.e2e.base.BaseTest;
import com.saigonplantravel.e2e.listeners.TestListener;
import com.saigonplantravel.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(TestListener.class)
public class LoginTest extends BaseTest {
    public LoginTest() {
        super(new ChromeDriverFactory());
    }

    @DataProvider(name = "loginData")
    public Object[][] loginData(){
        return new Object[][] {
                {"nguyenbatan.2908@gmail.com", "nguyenbatan999"}
        };
    }
    @Test(dataProvider = "loginData")
    public void loginTest(String email, String password) {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        loginPage.login(email, password);
        loginPage.waitForLoginSuccess();
        Assert.assertEquals(driver.getCurrentUrl(), "http://localhost:3000/");
    }
}
