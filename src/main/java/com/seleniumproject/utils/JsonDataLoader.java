package com.seleniumproject.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonDataLoader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonDataLoader() {
    }

    public static List<Map<String, String>> loadRecordsAsMap(String filePath) {
        try {
            JsonNode root = readJsonRoot(filePath);
            if (root == null || root.isNull()) {
                return Collections.emptyList();
            }

            if (root.isArray()) {
                List<Map<String, String>> records = new ArrayList<>();
                for (JsonNode node : root) {
                    if (node.isObject()) {
                        records.add(flattenRecord(node));
                    }
                }
                return Collections.unmodifiableList(records);
            }

            if (root.isObject()) {
                return Collections.singletonList(flattenRecord(root));
            }

            throw new IllegalArgumentException("JSON data must be an object or array of objects: " + filePath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read JSON data file: " + filePath, e);
        }
    }

    private static JsonNode readJsonRoot(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            return OBJECT_MAPPER.readTree(file);
        }

        InputStream classpathStream = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(filePath);
        if (classpathStream != null) {
            try (InputStream inputStream = classpathStream) {
                return OBJECT_MAPPER.readTree(inputStream);
            }
        }

        throw new IllegalArgumentException("Data file not found in filesystem or classpath: " + filePath);
    }

    /**
     * Flattens a single JSON record to Map<String, String>.
     * Primitive values are converted via asText().
     * Nested objects/arrays are serialized back to a JSON string so they can be
     * later parsed by TestContext.resolveMap().
     */
    private static Map<String, String> flattenRecord(JsonNode record) {
        Map<String, String> result = new LinkedHashMap<>();
        record.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isObject() || value.isArray()) {
                result.put(entry.getKey(), value.toString());
            } else if (value.isNull()) {
                result.put(entry.getKey(), "");
            } else {
                result.put(entry.getKey(), value.asText());
            }
        });
        return result;
    }

    public static int countRecords(String filePath) {
        return loadRecordsAsMap(filePath).size();
    }

    public static Map<String, String> loadRecordAsMap(String filePath, int index) {
        List<Map<String, String>> records = loadRecordsAsMap(filePath);
        if (records.isEmpty()) {
            return Collections.emptyMap();
        }

        int safeIndex = Math.floorMod(index, records.size());
        return records.get(safeIndex);
    }
}
