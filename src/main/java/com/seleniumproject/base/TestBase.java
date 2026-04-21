package com.seleniumproject.base;

import com.seleniumproject.pages.DashboardPageObjects;
import com.seleniumproject.pages.LoginPageObjects;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public class TestBase {
    private static final ThreadLocal<WebDriver> CURRENT_DRIVER = new ThreadLocal<>();

    public static WebDriver getCurrentDriver() {
        return CURRENT_DRIVER.get();
    }

    private static final int DEFAULT_VIEWPORT_WIDTH = 1920;
    private static final int DEFAULT_VIEWPORT_HEIGHT = 1080;
    private static final int DEFAULT_VIEWPORT_WIDTH_OFFSET = 16;
    private static final int DEFAULT_VIEWPORT_HEIGHT_OFFSET = 96;
    private static final long DEFAULT_NAVIGATION_TIMEOUT_MS = 60000;
    private static final int DEFAULT_NAVIGATION_RETRIES = 2;

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected Properties properties;

    public void setUp() {
        loadProperties();
        String browserName = getProperty("browser.name");
        boolean headless = resolveHeadlessMode();
        String viewportMode = getProperty("viewport.mode");
        int viewportWidth = resolveViewportWidth(viewportMode);
        int viewportHeight = resolveViewportHeight(viewportMode);

        System.out.println("Launching browser: " + browserName);
        System.out.println("Headless mode: " + headless);
        System.out.println("Viewport mode: " + (viewportMode == null || viewportMode.isBlank() ? "dynamic" : viewportMode));
        System.out.println("Viewport size: " + viewportWidth + "x" + viewportHeight);

        driver = createDriver(browserName, headless, viewportWidth, viewportHeight, viewportMode);
        CURRENT_DRIVER.set(driver);

        long timeoutMs = getLongProperty("navigation.timeout.ms", DEFAULT_NAVIGATION_TIMEOUT_MS);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(timeoutMs));
        driver.manage().timeouts().scriptTimeout(Duration.ofMillis(timeoutMs));
        wait = new WebDriverWait(driver, java.util.Objects.requireNonNull(Duration.ofMillis(timeoutMs)));
        applyWindowSizing(headless, viewportMode, viewportWidth, viewportHeight);
    }

    public void navigateToUrlWithRetry(String url) {
        long timeoutMs = getLongProperty("navigation.timeout.ms", DEFAULT_NAVIGATION_TIMEOUT_MS);
        int retries = getIntProperty("navigation.retry.count", DEFAULT_NAVIGATION_RETRIES);

        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                driver.navigate().to(url);
                waitForDocumentReady(timeoutMs);
                return;
            } catch (WebDriverException ex) {
                if (attempt == retries) {
                    throw ex;
                }
                System.out.println("Navigation attempt " + attempt + " failed for URL: " + url + ". Retrying...");
                sleepSilently(1000);
            }
        }
    }

    public LoginPageObjects loginPage() {
        return new LoginPageObjects(driver, wait);
    }

    public DashboardPageObjects dashboardPage() {
        return new DashboardPageObjects(driver, wait);
    }

    public String dataPath(String fileName) {
        return "env/" + System.getProperty("env", "dev") + "/data/" + fileName;
    }

    public void tearDown() {
        CURRENT_DRIVER.remove();
        if (driver != null) {
            driver.quit();
        }
        driver = null;
        wait = null;
    }

    private void loadProperties() {
        properties = new Properties();

        // Load common application defaults first
        String appPropsPath = "application.properties";
        try (InputStream appInput = getRequiredResourceAsStream(appPropsPath)) {
            properties.load(appInput);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties from: " + appPropsPath, e);
        }

        // Load environment-specific properties (overrides application.properties values)
        String env = System.getProperty("env", "dev");
        String envPath = "env/" + env + "/env.properties";
        System.out.println("========================================");
        System.out.println("Loading environment: " + env);
        System.out.println("Property file path: " + envPath);
        try (InputStream input = getRequiredResourceAsStream(envPath)) {
            properties.load(input);
            System.out.println("App URL: " + properties.getProperty("app.url"));
            System.out.println("Browser: " + properties.getProperty("browser.name"));
            System.out.println("========================================");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load env properties from: " + envPath, e);
        }
    }

    private InputStream getRequiredResourceAsStream(String path) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new IllegalStateException("Classpath resource not found: " + path);
        }
        return inputStream;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    private boolean resolveHeadlessMode() {
        String headlessOverride = System.getProperty("headless");
        if (headlessOverride != null && !headlessOverride.isBlank()) {
            return Boolean.parseBoolean(headlessOverride.trim());
        }

        // When using Selenium Grid, never force headless.
        // The Grid's standalone-chrome container has its own Xvfb display and VNC server;
        // running Chrome headless bypasses that display and breaks video recording.
        if (resolveGridUrl() != null) {
            return false;
        }

        // For non-Grid CI runs, force headless (no display available)
        if ("true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"))) {
            return true;
        }
        if ("true".equalsIgnoreCase(System.getenv("CI"))) {
            return true;
        }

        return Boolean.parseBoolean(getProperty("headless"));
    }

    private int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid value for " + key + ": " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    private long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid value for " + key + ": " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    private String resolveGridUrl() {
        // System property takes highest priority: -Dgrid.url=http://localhost:4444
        String sysProp = System.getProperty("grid.url");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        // Fall back to properties file (safe default: commented out in env.properties)
        String propFile = getProperty("grid.url");
        if (propFile != null && !propFile.isBlank()) {
            return propFile.trim();
        }
        return null;
    }

    private WebDriver createDriver(String browserName, boolean headless, int viewportWidth, int viewportHeight, String viewportMode) {
        String normalizedBrowser = browserName == null ? "chrome" : browserName.trim().toLowerCase();
        String gridUrl = resolveGridUrl();

        if (gridUrl != null && !gridUrl.isBlank()) {
            System.out.println("Grid URL: " + gridUrl);
            return createRemoteDriver(normalizedBrowser, headless, viewportWidth, viewportHeight, viewportMode, gridUrl);
        }

        try {
            switch (normalizedBrowser) {
                case "firefox":
                    return new FirefoxDriver(buildFirefoxOptions(headless, viewportWidth, viewportHeight));
                case "edge":
                    return new EdgeDriver(buildEdgeOptions(headless, viewportWidth, viewportHeight, viewportMode));
                case "chromium":
                case "chrome":
                default:
                    return new ChromeDriver(buildChromeOptions(headless, viewportWidth, viewportHeight, viewportMode));
            }
        } catch (Exception e) {
            System.out.println("Failed to launch " + normalizedBrowser + " browser: " + e.getMessage());
            throw e;
        }
    }

    private WebDriver createRemoteDriver(String browserName, boolean headless, int viewportWidth, int viewportHeight, String viewportMode, String gridUrl) {
        try {
            URL remoteUrl = URI.create(gridUrl).toURL();
            switch (browserName) {
                case "firefox":
                    return new RemoteWebDriver(remoteUrl, buildFirefoxOptions(headless, viewportWidth, viewportHeight));
                case "edge":
                    return new RemoteWebDriver(remoteUrl, buildEdgeOptions(headless, viewportWidth, viewportHeight, viewportMode));
                case "chromium":
                case "chrome":
                default:
                    return new RemoteWebDriver(remoteUrl, buildChromeOptions(headless, viewportWidth, viewportHeight, viewportMode));
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Selenium Grid URL: " + gridUrl, e);
        }
    }

    private ChromeOptions buildChromeOptions(boolean headless, int viewportWidth, int viewportHeight, String viewportMode) {
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        if (headless) {
            options.addArguments("--headless=new");
        }
        if (headless || isDynamicViewport(viewportMode)) {
            options.addArguments("--window-size=" + viewportWidth + "," + viewportHeight);
        }
        return options;
    }

    private EdgeOptions buildEdgeOptions(boolean headless, int viewportWidth, int viewportHeight, String viewportMode) {
        EdgeOptions options = new EdgeOptions();
        options.setAcceptInsecureCerts(true);
        if (headless) {
            options.addArguments("--headless=new");
        }
        if (headless || isDynamicViewport(viewportMode)) {
            options.addArguments("--window-size=" + viewportWidth + "," + viewportHeight);
        }
        return options;
    }

    private FirefoxOptions buildFirefoxOptions(boolean headless, int viewportWidth, int viewportHeight) {
        FirefoxOptions options = new FirefoxOptions();
        options.setAcceptInsecureCerts(true);
        if (headless) {
            options.addArguments("-headless");
        }
        options.addArguments("--width=" + viewportWidth);
        options.addArguments("--height=" + viewportHeight);
        return options;
    }

    private void applyWindowSizing(boolean headless, String viewportMode, int viewportWidth, int viewportHeight) {
        if (driver == null || headless) {
            return;
        }

        if (isDynamicViewport(viewportMode)) {
            driver.manage().window().maximize();
            return;
        }

        driver.manage().window().setSize(new org.openqa.selenium.Dimension(viewportWidth, viewportHeight));
    }

    private void waitForDocumentReady(long timeoutMs) {
        new WebDriverWait(driver, Duration.ofMillis(timeoutMs)).until(webDriver ->
            "complete".equals(((JavascriptExecutor) webDriver).executeScript("return document.readyState"))
        );
    }

    private int resolveViewportWidth(String viewportMode) {
        if (!isDynamicViewport(viewportMode)) {
            return getIntProperty("viewport.width", DEFAULT_VIEWPORT_WIDTH);
        }

        try {
            Rectangle usableBounds = getUsableScreenBounds();
            int widthOffset = getIntProperty("viewport.width.offset", DEFAULT_VIEWPORT_WIDTH_OFFSET);
            return Math.max(1280, usableBounds.width - widthOffset);
        } catch (HeadlessException e) {
            System.out.println("Headless environment detected. Using default viewport width 1920.");
            return DEFAULT_VIEWPORT_WIDTH;
        }
    }

    private int resolveViewportHeight(String viewportMode) {
        if (!isDynamicViewport(viewportMode)) {
            return getIntProperty("viewport.height", DEFAULT_VIEWPORT_HEIGHT);
        }

        try {
            Rectangle usableBounds = getUsableScreenBounds();
            int heightOffset = getIntProperty("viewport.height.offset", DEFAULT_VIEWPORT_HEIGHT_OFFSET);
            return Math.max(720, usableBounds.height - heightOffset);
        } catch (HeadlessException e) {
            System.out.println("Headless environment detected. Using default viewport height 1080.");
            return DEFAULT_VIEWPORT_HEIGHT;
        }
    }

    private boolean isDynamicViewport(String viewportMode) {
        return viewportMode == null || viewportMode.isBlank() || viewportMode.equalsIgnoreCase("dynamic");
    }

    private void sleepSilently(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to retry browser navigation.", e);
        }
    }

    private Rectangle getUsableScreenBounds() {
        try {
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        } catch (HeadlessException e) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            return new Rectangle((int) screenSize.getWidth(), (int) screenSize.getHeight());
        }
    }

    public WebDriver getDriver() {
        return driver;
    }

    public WebDriverWait getWait() {
        return wait;
    }
}