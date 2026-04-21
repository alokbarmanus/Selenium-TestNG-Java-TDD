package com.seleniumproject.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPageObjects {

    public static final By USERNAME_INPUT = By.cssSelector("input[name='username']");
    public static final By PASSWORD_INPUT = By.cssSelector("input[name='password']");
    public static final By LOGIN_BUTTON = By.cssSelector("button[type='submit']");

    private final WebDriverWait wait;

    public LoginPageObjects(WebDriver driver, WebDriverWait wait) {
        this.wait = wait;
    }

    public boolean isPageLoaded() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME_INPUT)).isDisplayed();
    }

    public WebElement usernameField() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(USERNAME_INPUT));
    }

    public WebElement passwordField() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_INPUT));
    }

    public void enterUsername(String username) {
        WebElement element = usernameField();
        element.clear();
        element.sendKeys(username);
    }

    public void enterPassword(String password) {
        WebElement element = passwordField();
        element.clear();
        element.sendKeys(password);
    }

    public void clickOnLoginButton() {
        wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON)).click();
    }

    public String getEnteredUsername() {
        return usernameField().getDomProperty("value");
    }

    public String getEnteredPassword() {
        return passwordField().getDomProperty("value");
    }

    public void loginToApplication(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickOnLoginButton();
    }
    
}