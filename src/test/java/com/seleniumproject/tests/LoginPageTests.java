package com.seleniumproject.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumproject.base.TestBase;
import com.seleniumproject.utils.JsonDataLoader;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

public class LoginPageTests extends TestBase {

    @BeforeMethod
    public void setUp() {
        super.setUp();
        navigateToUrlWithRetry(getProperty("app.url"));
        Assert.assertTrue(loginPage().isPageLoaded(), "Login page did not load.");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testLoginWithJsonData() {
        Map<String, String> data = JsonDataLoader.loadRecordAsMap(dataPath("loginData.json"), 0);
        loginPage().enterUsername(data.get("username"));
        loginPage().enterPassword(data.get("password"));
        loginPage().clickOnLoginButton();
        Assert.assertTrue(dashboardPage().waitForDashboardPage(), "Dashboard page did not load after login.");
    }

    @Test
    public void testLoginWithMapData() {
        Map<String, String> record = JsonDataLoader.loadRecordAsMap(dataPath("loginDataMap.json"), 0);
        Map<String, String> credentials = parseNestedJson(record.get("loginDataAdmin"));
        loginPage().enterUsername(credentials.get("username"));
        loginPage().enterPassword(credentials.get("password"));
        loginPage().clickOnLoginButton();
        Assert.assertTrue(dashboardPage().waitForDashboardPage(), "Dashboard page did not load after login.");
    }

    @Test
    public void testLoginWithHardcodedCredentials() {
        loginPage().enterUsername("Admin");
        loginPage().enterPassword("admin123");
        loginPage().clickOnLoginButton();
        Assert.assertTrue(dashboardPage().waitForDashboardPage(), "Dashboard page did not load after login.");
    }

    private Map<String, String> parseNestedJson(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse nested JSON: " + json, e);
        }
    }
}
