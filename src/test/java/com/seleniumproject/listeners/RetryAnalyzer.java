package com.seleniumproject.listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Retries failed TestNG test methods up to {@code retry.count} times.
 * The limit is read from application.properties (env-specific env.properties overrides it).
 * Configure via src/test/resources/application.properties:
 *   retry.count=1   → retry once (2 total attempts)
 *   retry.count=0   → no retry (default safe behaviour)
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRY = loadRetryCount();
    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY) {
            retryCount++;
            System.out.printf("[Retry] '%s' failed — retrying (attempt %d of %d)%n",
                    result.getName(), retryCount, MAX_RETRY);
            return true;
        }
        return false;
    }

    // ── property loading ──────────────────────────────────────────────────────

    private static int loadRetryCount() {
        Properties props = new Properties();

        // 1. Load base defaults from application.properties
        loadFromClasspath("application.properties", props);

        // 2. Load env-specific overrides (env.properties takes precedence)
        String env = System.getProperty("env", "dev");
        loadFromClasspath("env/" + env + "/env.properties", props);

        String value = props.getProperty("retry.count", "0").trim();
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            System.out.println("[RetryAnalyzer] Invalid retry.count value '" + value + "', defaulting to 0.");
            return 0;
        }
    }

    private static void loadFromClasspath(String path, Properties target) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                Properties loaded = new Properties();
                loaded.load(in);
                // Only override keys that are present in the file being loaded
                loaded.stringPropertyNames().forEach(key -> target.setProperty(key, loaded.getProperty(key)));
            }
        } catch (IOException e) {
            System.out.println("[RetryAnalyzer] Could not read '" + path + "': " + e.getMessage());
        }
    }
}
