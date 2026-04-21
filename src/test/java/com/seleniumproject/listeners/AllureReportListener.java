package com.seleniumproject.listeners;

import com.seleniumproject.base.TestBase;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;

/**
 * Allure TestNG listener — attaches screenshots on test failure and exposes
 * a static helper so Hooks can attach step-level screenshots to Allure too.
 */
public class AllureReportListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        attachScreenshot("Failure Screenshot");
    }

    /**
     * Captures the current browser screenshot and attaches it to the active
     * Allure test. Safe to call from Hooks ({@literal @AfterStep}) or anywhere
     * a driver is available.
     */
    public static void attachScreenshot(String name) {
        WebDriver driver = TestBase.getCurrentDriver();
        if (driver instanceof TakesScreenshot ts) {
            byte[] bytes = ts.getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(bytes), "png");
        }
    }
}
