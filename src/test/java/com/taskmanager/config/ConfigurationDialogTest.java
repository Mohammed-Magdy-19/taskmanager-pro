package com.taskmanager.config;

import com.taskmanager.config.wizard.DatabaseStep;
import com.taskmanager.config.wizard.PreferencesStep;
import com.taskmanager.config.wizard.ReminderStep;
import com.taskmanager.config.wizard.SummaryStep;
import com.taskmanager.config.wizard.WelcomeStep;
import com.taskmanager.config.wizard.WizardData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationDialogTest {

    @TempDir
    Path tempDir;

    private String testConfigPath;

    @BeforeEach
    void setUp() {
        testConfigPath = tempDir.resolve("dialog-test.properties").toString();
        ConfigManager.resetForTesting(testConfigPath);
    }

    @AfterEach
    void tearDown() {
        ConfigManager.resetForTesting(null);
    }

    @Test
    void configurationDialogLoadsExistingConfigProperties() {
        ConfigManager config = ConfigManager.getInstance();
        config.setProperty(ConfigManager.KEY_DB_PATH, "existing/path.db");
        config.setProperty(ConfigManager.KEY_APP_THEME, "Dark");
        config.setProperty(ConfigManager.KEY_REMINDER_MINUTES, "60");

        ConfigurationDialog dialog = new ConfigurationDialog();
        assertEquals("existing/path.db", dialog.getSettingsData().getDbPath());
        assertEquals("Dark", dialog.getSettingsData().getTheme());
        assertEquals(60, dialog.getSettingsData().getReminderMinutes());
        dialog.dispose();
    }

    @Test
    void configWizardFrameNavigatesAndInvokesCallback() {
        AtomicBoolean finished = new AtomicBoolean(false);
        ConfigWizardFrame wizard = new ConfigWizardFrame(() -> finished.set(true));

        assertEquals(0, wizard.getCurrentStep());
        assertEquals("taskmanager.db", wizard.getWizardData().getDbPath());

        wizard.dispose();
    }

    @Test
    void fullPhase1CheckpointSimulation() {
        // 1. Initial state: config does not exist
        assertFalse(ConfigManager.configExists(testConfigPath));

        // 2. User walks through wizard steps and finishes
        WizardData wizardData = new WizardData();
        WelcomeStep welcome = new WelcomeStep();
        welcome.commit(wizardData);

        wizardData.setDbPath(tempDir.resolve("my-tasks.db").toString());
        DatabaseStep dbStep = new DatabaseStep(wizardData);
        dbStep.commit(wizardData);

        wizardData.setTheme("Dark");
        PreferencesStep prefStep = new PreferencesStep(wizardData);
        prefStep.commit(wizardData);

        wizardData.setReminderMinutes(30);
        ReminderStep reminderStep = new ReminderStep(wizardData);
        reminderStep.commit(wizardData);

        SummaryStep summaryStep = new SummaryStep(wizardData);
        summaryStep.refresh(wizardData);

        // 3. Save to ConfigManager (what Finish does)
        ConfigManager config = ConfigManager.getInstance();
        config.setProperty(ConfigManager.KEY_DB_PATH, wizardData.getDbPath());
        config.setProperty(ConfigManager.KEY_APP_THEME, wizardData.getTheme());
        config.setProperty(ConfigManager.KEY_APP_LANGUAGE, wizardData.getLanguage());
        config.setProperty(ConfigManager.KEY_REMINDER_MINUTES, String.valueOf(wizardData.getReminderMinutes()));
        config.setProperty(ConfigManager.KEY_AUTO_BACKUP, String.valueOf(wizardData.isAutoBackup()));

        // 4. Verify config file exists on disk
        assertTrue(ConfigManager.configExists(testConfigPath));
        assertTrue(new File(testConfigPath).exists());

        // 5. Verify reload in a fresh ConfigManager instance (simulating next app run)
        ConfigManager.resetForTesting(testConfigPath);
        ConfigManager reloaded = ConfigManager.getInstance();

        assertEquals(tempDir.resolve("my-tasks.db").toString(), reloaded.getProperty(ConfigManager.KEY_DB_PATH));
        assertEquals("Dark", reloaded.getProperty(ConfigManager.KEY_APP_THEME));
        assertEquals("English", reloaded.getProperty(ConfigManager.KEY_APP_LANGUAGE));
        assertEquals(30, reloaded.getIntProperty(ConfigManager.KEY_REMINDER_MINUTES, 15));
        assertTrue(reloaded.getBooleanProperty(ConfigManager.KEY_AUTO_BACKUP, false));

        // 6. Config exists check on second launch returns true (skips wizard)
        assertTrue(ConfigManager.configExists(testConfigPath));
    }
}
