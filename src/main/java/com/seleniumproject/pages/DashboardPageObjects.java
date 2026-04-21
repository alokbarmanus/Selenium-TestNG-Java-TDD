package com.seleniumproject.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.Objects;

public class DashboardPageObjects {

    private static final By DASHBOARD_HEADER = By.xpath("//h6[@class='oxd-text oxd-text--h6 oxd-topbar-header-breadcrumb-module']");
    private static final By IMG_ORANGEHRM = By.xpath("//img[@alt='client brand banner']");
    private static final By INPUT_SEARCH = By.cssSelector("input[placeholder='Search']");
    private static final By LABEL_PANELOPTIONS = By.xpath("//ul[@class='oxd-main-menu']/li");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public DashboardPageObjects(WebDriver driver, WebDriverWait wait) {
        this.driver = Objects.requireNonNull(driver, "Selenium WebDriver is null. Ensure browser setup is completed before using DashboardPageObjects.");
        this.wait = Objects.requireNonNull(wait, "WebDriverWait is null. Ensure browser setup is completed before using DashboardPageObjects.");
    }

    public boolean isPageReady() {
        try {
            return !driver.getWindowHandles().isEmpty();
        } catch (WebDriverException e) {
            return false;
        }
    }

    public boolean waitForDashboardPage() {
        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/dashboard"),
            ExpectedConditions.visibilityOfElementLocated(DASHBOARD_HEADER)
        ));
        return isDashboardHeaderVisible();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getHomePageTitle() {
        return driver.getTitle();
    }

    public boolean isDashboardHeaderVisible() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(DASHBOARD_HEADER)).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    public String getPageHeader() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(DASHBOARD_HEADER)).getText().trim();
    }

    public boolean getDashboardPageImage() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(IMG_ORANGEHRM)).isDisplayed();
    }

    public boolean getSearchTextField() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(INPUT_SEARCH)).isDisplayed();
    }

    public void verifySidePanelOptions(List<String> expectedOptions) {
        List<String> actualOptions = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(LABEL_PANELOPTIONS))
            .stream()
            .map(WebElement::getText)
            .map(String::trim)
            .filter(option -> !option.isBlank())
            .toList();

        if (!actualOptions.containsAll(expectedOptions)) {
            throw new AssertionError("Expected side panel options not found. Expected: " + expectedOptions + ", Actual: " + actualOptions);
        }
    }
}
