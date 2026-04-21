package com.seleniumproject.tests;

import com.seleniumproject.base.TestBase;
import com.seleniumproject.constants.AppEnum;
import com.seleniumproject.utils.JsonDataLoader;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

public class DashboardPageTests extends TestBase {

    @BeforeMethod
    public void setUp() {
        super.setUp();
        navigateToUrlWithRetry(getProperty("app.url"));
        Map<String, String> data = JsonDataLoader.loadRecordAsMap(dataPath("loginData.json"), 0);
        loginPage().enterUsername(data.get("username"));
        loginPage().enterPassword(data.get("password"));
        loginPage().clickOnLoginButton();
        Assert.assertTrue(dashboardPage().waitForDashboardPage(), "Dashboard page did not load.");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testDashboardPageHeader() {
        Assert.assertEquals(dashboardPage().getPageHeader(), AppEnum.DASHBOARD_HEADER,
            "Dashboard page header does not match expected value.");
    }

    @Test
    public void testDashboardSidePanelElements() {
        Assert.assertTrue(dashboardPage().getDashboardPageImage(),
            "OrangeHRM image is not visible in the left side panel.");
        Assert.assertTrue(dashboardPage().getSearchTextField(),
            "Search box is not visible in the left side panel.");
    }

    @Test
    public void testDashboardSidePanelOptions() {
        List<String> expectedOptions = List.of(
            "Admin", "PIM", "Time", "Recruitment",
            "My Info", "Performance", "Dashboard", "Directory", "Maintenance", "Claim", "Buzz"
        );
        dashboardPage().verifySidePanelOptions(expectedOptions);
    }
}
