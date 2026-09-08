package com.taskmanager.config;

import com.taskmanager.util.AppLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Singleton configuration manager providing typed access to persistent application settings.
 * Backed by a standard properties file so settings persist across launches without needing
 * a running database.
 */
public class ConfigManager {

    public static final String DEFAULT_CONFIG_FILE = "config.properties";

    public static final String KEY_DB_PATH = "db.path";
    public static final String KEY_APP_THEME = "app.theme";
    public static final String KEY_APP_LANGUAGE = "app.language";
    public static final String KEY_REMINDER_MINUTES = "reminder.minutes";
    public static final String KEY_AUTO_BACKUP = "app.autoBackup";

    private static final AppLogger LOGGER = AppLogger.getLogger(ConfigManager.class);
    private static ConfigManager instance;

    private final Properties properties = new Properties();
    private String configPath;

    /**
     * Private constructor enforcing Singleton lifecycle.
     * Automatically triggers initial config load.
     */
    private ConfigManager() {
        this(DEFAULT_CONFIG_FILE);
    }

    /**
     * Package-private constructor allowing custom file location for test isolation.
     *
     * @param configPath relative or absolute file path to the properties file
     */
    ConfigManager(String configPath) {
        this.configPath = (configPath == null || configPath.trim().isEmpty())
                ? DEFAULT_CONFIG_FILE
                : configPath.trim();
        loadConfig();
    }

    /**
     * Retrieves the global thread-safe {@link ConfigManager} instance.
     *
     * @return the singleton configuration manager
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Checks if the default configuration file exists on disk.
     * Used at startup to determine whether the first-run configuration wizard is needed.
     *
     * @return true if the configuration file is present, false otherwise
     */
    public static boolean configExists() {
        return configExists(DEFAULT_CONFIG_FILE);
    }

    /**
     * Checks if a configuration file exists at a given path.
     *
     * @param path the file path to verify
     * @return true if the file exists, false if absent or null
     */
    public static boolean configExists(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        return new File(path.trim()).exists();
    }

    /**
     * Resets the singleton instance and points to an isolated file path.
     * Package-private to avoid test state leakage between test executions.
     *
     * @param testPath the file path to use for test execution, or null to reset to default
     */
    static synchronized void resetForTesting(String testPath) {
        if (testPath == null) {
            instance = null;
        } else {
            instance = new ConfigManager(testPath);
        }
    }

    /**
     * Loads properties from the active config file path.
     * Leaves existing in-memory properties untouched if the file does not exist yet.
     */
    private void loadConfig() {
        File file = new File(configPath);
        if (!file.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
        } catch (IOException e) {
            LOGGER.error("Failed to read configuration file: " + configPath, e);
        }
    }

    /**
     * Persists in-memory properties to the file system immediately.
     * Protects user settings against crashes or abrupt shutdowns.
     */
    public synchronized void saveConfig() {
        File file = new File(configPath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, "Task Manager Configuration");
        } catch (IOException e) {
            LOGGER.error("Failed to write configuration file: " + configPath, e);
        }
    }

    /**
     * Fetches a string configuration property.
     *
     * @param key the property identifier
     * @return the value associated with the key, or null if missing
     */
    public synchronized String getProperty(String key) {
        if (key == null) {
            return null;
        }
        return properties.getProperty(key);
    }

    /**
     * Fetches a string configuration property with a fallback default.
     *
     * @param key the property identifier
     * @param defaultValue the fallback value if the key is not defined
     * @return the stored value or defaultValue
     */
    public synchronized String getProperty(String key, String defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Fetches an integer configuration value with fallback on parsing errors or missing keys.
     *
     * @param key the property identifier
     * @param defaultValue the fallback integer value
     * @return parsed integer or defaultValue if absent/malformed
     */
    public synchronized int getIntProperty(String key, int defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid integer for config key '" + key + "': " + value + ". Using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Fetches a boolean configuration value with fallback.
     *
     * @param key the property identifier
     * @param defaultValue the fallback boolean value
     * @return parsed boolean or defaultValue if absent
     */
    public synchronized boolean getBooleanProperty(String key, boolean defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Sets a configuration property and immediately persists it to disk.
     *
     * @param key the property identifier (must not be null)
     * @param value the property value (must not be null)
     * @throws IllegalArgumentException if key or value is null
     */
    public synchronized void setProperty(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Config key and value cannot be null");
        }
        properties.setProperty(key, value);
        saveConfig();
    }

    /**
     * Returns the active configuration file path.
     *
     * @return current config file path
     */
    public String getConfigPath() {
        return configPath;
    }
}