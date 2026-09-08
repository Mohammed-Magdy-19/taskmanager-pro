package com.taskmanager.config.wizard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WizardStepTest {

    private WizardData wizardData;

    @BeforeEach
    void setUp() {
        wizardData = new WizardData();
    }

    @Test
    void wizardDataHasSensibleDefaults() {
        assertEquals("taskmanager.db", wizardData.getDbPath());
        assertEquals("Light", wizardData.getTheme());
        assertEquals("English", wizardData.getLanguage());
        assertEquals(15, wizardData.getReminderMinutes());
        assertTrue(wizardData.isAutoBackup());
    }

    @Test
    void wizardDataGettersAndSettersWork() {
        wizardData.setDbPath("custom.db");
        wizardData.setTheme("Dark");
        wizardData.setLanguage("English");
        wizardData.setReminderMinutes(30);
        wizardData.setAutoBackup(false);

        assertEquals("custom.db", wizardData.getDbPath());
        assertEquals("Dark", wizardData.getTheme());
        assertEquals("English", wizardData.getLanguage());
        assertEquals(30, wizardData.getReminderMinutes());
        assertFalse(wizardData.isAutoBackup());
    }

    @Test
    void databaseStepCommitsToWizardData() {
        wizardData.setDbPath("initial.db");
        DatabaseStep step = new DatabaseStep(wizardData);

        wizardData.setDbPath("modified.db");
        step.refresh(wizardData);

        WizardData target = new WizardData();
        step.commit(target);

        assertEquals("modified.db", target.getDbPath());
    }

    @Test
    void preferencesStepCommitsToWizardData() {
        wizardData.setTheme("Dark");
        wizardData.setAutoBackup(false);

        PreferencesStep step = new PreferencesStep(wizardData);
        WizardData target = new WizardData();
        step.commit(target);

        assertEquals("Dark", target.getTheme());
        assertEquals("English", target.getLanguage());
        assertFalse(target.isAutoBackup());
    }

    @Test
    void reminderStepCommitsToWizardData() {
        wizardData.setReminderMinutes(45);
        ReminderStep step = new ReminderStep(wizardData);

        WizardData target = new WizardData();
        step.commit(target);

        assertEquals(45, target.getReminderMinutes());
    }

    @Test
    void summaryStepRefreshesWithoutError() {
        SummaryStep step = new SummaryStep(wizardData);
        assertDoesNotThrow(() -> step.refresh(wizardData));
        assertDoesNotThrow(() -> step.commit(wizardData));
    }
}
