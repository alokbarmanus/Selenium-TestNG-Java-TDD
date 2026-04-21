package com.seleniumproject.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RuntimeMemoryManager {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeMemoryManager.class);

    private static final ThreadLocal<Map<String, String>> SCENARIO_MEMORY =
        ThreadLocal.withInitial(HashMap::new);

    private RuntimeMemoryManager() {
    }

    public static void setPropertyInMemory(String propertyKey, String propertyValue) {
        if (propertyKey == null || propertyKey.isBlank()) {
            throw new IllegalArgumentException("propertyKey must not be blank");
        }
        SCENARIO_MEMORY.get().put(propertyKey, propertyValue);
        LOG.info("Runtime property set: {}={}", propertyKey, propertyValue);
    }

    public static String getPropertyFromMemory(String propertyKey) {
        if (propertyKey == null || propertyKey.isBlank()) {
            throw new IllegalArgumentException("propertyKey must not be blank");
        }
        return SCENARIO_MEMORY.get().get(propertyKey);
    }

    public static boolean containsProperty(String propertyKey) {
        if (propertyKey == null || propertyKey.isBlank()) {
            return false;
        }
        return SCENARIO_MEMORY.get().containsKey(propertyKey);
    }

    public static Map<String, String> getAllProperties() {
        return Collections.unmodifiableMap(new HashMap<>(SCENARIO_MEMORY.get()));
    }

    public static void clear() {
        SCENARIO_MEMORY.remove();
    }
}
