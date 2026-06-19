package com.automation.twitch.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new ExceptionInInitializerError("config.properties not found in classpath");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load config.properties: " + e.getMessage());
        }
    }

    private ConfigManager() {}

    // System properties override config file values — useful for CI overrides
    public static String get(String key) {
        String value = System.getProperty(key, properties.getProperty(key));
        if (value == null) {
            throw new IllegalArgumentException("Required property '" + key + "' not found in config");
        }
        return value.trim();
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static long getLong(String key) {
        return Long.parseLong(get(key));
    }
}
