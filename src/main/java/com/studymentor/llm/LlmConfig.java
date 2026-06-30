package com.studymentor.llm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public record LlmConfig(String baseUrl, String apiKey, String model, double temperature) {

    public static LlmConfig fromEnv() {
        Map<String, String> dotEnv = readDotEnv();
        String baseUrl = readConfig("LLM_BASE_URL", "", dotEnv);
        String apiKey = readConfig("LLM_API_KEY", "", dotEnv);
        String model = readConfig("LLM_MODEL", "mock", dotEnv);
        double temperature = readDoubleConfig("LLM_TEMPERATURE", 0.3, dotEnv);
        return new LlmConfig(baseUrl, apiKey, model, temperature);
    }

    public void validateForRealClient() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("LLM_BASE_URL is required when using --real");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("LLM_API_KEY is required when using --real");
        }
        if (model == null || model.isBlank() || "mock".equals(model)) {
            throw new IllegalStateException("LLM_MODEL is required when using --real");
        }
    }

    private static String readConfig(String name, String defaultValue, Map<String, String> dotEnv) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = dotEnv.get(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static double readDoubleConfig(String name, double defaultValue, Map<String, String> dotEnv) {
        String value = readConfig(name, "", dotEnv);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static Map<String, String> readDotEnv() {
        Path path = Path.of(".env");
        if (!Files.exists(path)) {
            return Map.of();
        }

        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, equalsIndex).trim();
                String value = trimmed.substring(equalsIndex + 1).trim();
                values.put(key, stripQuotes(value));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read .env file", e);
        }
        return values;
    }

    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
