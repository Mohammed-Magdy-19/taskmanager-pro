package com.taskmanager.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @TempDir
    Path tempDir;

    private String testConfigPath;
    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        testConfigPath = tempDir.resolve("test-config.properties").toString();
        ConfigManager.resetForTesting(testConfigPath);
        configManager = ConfigManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        ConfigManager.resetForTesting(null);
    }

    // --- Basic Existence & Lifecycle ---

    @Test
    void configDoesNotExistInitially() {
        assertFalse(ConfigManager.configExists(testConfigPath));
    }

    @Test
    void configExistsReturnsFalseForNullAndEmptyPath() {
        assertFalse(ConfigManager.configExists(null));
        assertFalse(ConfigManager.configExists(""));
        assertFalse(ConfigManager.configExists("   "));
    }

    @Test
    void configExistsReturnsTrueAfterPropertySaved() {
        configManager.setProperty(ConfigManager.KEY_DB_PATH, "test.db");
        assertTrue(ConfigManager.configExists(testConfigPath));
    }

    // --- Happy Paths & Persistence ---

    @Test
    void setPropertyPersistsToDiskAndReloadsInNewInstance() {
        configManager.setProperty(ConfigManager.KEY_DB_PATH, "custom/path/task.db");
        configManager.setProperty(ConfigManager.KEY_APP_THEME, "Dark");

        // Simulate fresh restart
        ConfigManager.resetForTesting(testConfigPath);
        ConfigManager reloaded = ConfigManager.getInstance();

        assertEquals("custom/path/task.db", reloaded.getProperty(ConfigManager.KEY_DB_PATH));
        assertEquals("Dark", reloaded.getProperty(ConfigManager.KEY_APP_THEME));
    }

    @Test
    void overwritePropertyUpdatesValue() {
        configManager.setProperty("key1", "original");
        assertEquals("original", configManager.getProperty("key1"));

        configManager.setProperty("key1", "updated");
        assertEquals("updated", configManager.getProperty("key1"));
    }

    // --- Boundary Values ---

    @Test
    void handlesEmptyAndWhitespaceStringValues() {
        configManager.setProperty("empty.key", "");
        configManager.setProperty("blank.key", "   ");

        assertEquals("", configManager.getProperty("empty.key"));
        assertEquals("   ", configManager.getProperty("blank.key"));
    }

    @Test
    void getIntPropertyHandlesBoundaryValues() {
        configManager.setProperty("zero", "0");
        configManager.setProperty("negative", "-99");
        configManager.setProperty("max", String.valueOf(Integer.MAX_VALUE));
        configManager.setProperty("min", String.valueOf(Integer.MIN_VALUE));

        assertEquals(0, configManager.getIntProperty("zero", 10));
        assertEquals(-99, configManager.getIntProperty("negative", 10));
        assertEquals(Integer.MAX_VALUE, configManager.getIntProperty("max", 0));
        assertEquals(Integer.MIN_VALUE, configManager.getIntProperty("min", 0));
    }

    @Test
    void getIntPropertyTrimsWhitespaceAroundNumber() {
        configManager.setProperty("padded.int", "  42  ");
        assertEquals(42, configManager.getIntProperty("padded.int", 0));
    }

    // --- Null / Missing Inputs ---

    @Test
    void getPropertyReturnsDefaultWhenKeyMissing() {
        assertNull(configManager.getProperty("missing.key"));
        assertEquals("defaultVal", configManager.getProperty("missing.key", "defaultVal"));
        assertNull(configManager.getProperty("missing.key", null));
    }

    @Test
    void getIntPropertyReturnsDefaultWhenKeyMissing() {
        assertEquals(25, configManager.getIntProperty("missing.int", 25));
    }

    @Test
    void getBooleanPropertyReturnsDefaultWhenKeyMissing() {
        assertTrue(configManager.getBooleanProperty("missing.bool", true));
        assertFalse(configManager.getBooleanProperty("missing.bool", false));
    }

    @Test
    void nullKeyReturnsDefaultSafely() {
        assertNull(configManager.getProperty(null));
        assertEquals("fallback", configManager.getProperty(null, "fallback"));
        assertEquals(100, configManager.getIntProperty(null, 100));
        assertTrue(configManager.getBooleanProperty(null, true));
    }

    @Test
    void setPropertyThrowsOnNullKeyOrValue() {
        assertThrows(IllegalArgumentException.class, () -> configManager.setProperty(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> configManager.setProperty("key", null));
    }

    // --- Invalid / Malformed Inputs ---

    @Test
    void getIntPropertyFallsBackOnMalformedNumber() {
        configManager.setProperty("bad.int.1", "abc");
        configManager.setProperty("bad.int.2", "12.34");
        configManager.setProperty("bad.int.3", "");

        assertEquals(15, configManager.getIntProperty("bad.int.1", 15));
        assertEquals(15, configManager.getIntProperty("bad.int.2", 15));
        assertEquals(15, configManager.getIntProperty("bad.int.3", 15));
    }

    @Test
    void getBooleanPropertyParsesStoredValues() {
        configManager.setProperty("flag.true", "true");
        configManager.setProperty("flag.TRUE", "TRUE");
        configManager.setProperty("flag.false", "false");
        configManager.setProperty("flag.invalid", "not_a_bool");

        assertTrue(configManager.getBooleanProperty("flag.true", false));
        assertTrue(configManager.getBooleanProperty("flag.TRUE", false));
        assertFalse(configManager.getBooleanProperty("flag.false", true));
        assertFalse(configManager.getBooleanProperty("flag.invalid", true));
    }

    // --- State & Path Edge Cases ---

    @Test
    void createsDirectoriesIfConfigFileInNestedSubdirectory() {
        Path nestedPath = tempDir.resolve("deep").resolve("nested").resolve("custom.properties");
        ConfigManager.resetForTesting(nestedPath.toString());
        ConfigManager nestedManager = ConfigManager.getInstance();

        nestedManager.setProperty("nested.key", "nested.val");

        assertTrue(new File(nestedPath.toString()).exists());
        assertEquals("nested.val", nestedManager.getProperty("nested.key"));
    }

    @Test
    void loadConfigDoesNotCrashOnExistingUnreadableOrEmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.properties").toFile();
        assertTrue(emptyFile.createNewFile());

        ConfigManager.resetForTesting(emptyFile.getAbsolutePath());
        ConfigManager emptyManager = ConfigManager.getInstance();

        assertNull(emptyManager.getProperty("any.key"));
    }
}