package com.taskmanager.config.wizard;

/**
 * Data transfer object holding temporary configuration values during the setup wizard flow.
 * Changes are accumulated here and only persisted to {@link com.taskmanager.config.ConfigManager}
 * once the user confirms and clicks Finish.
 */
public class WizardData {

    private String dbPath = "taskmanager.db";
    private String theme = "Light";
    private String language = "English";
    private int reminderMinutes = 15;
    private boolean autoBackup = true;

    /**
     * Default constructor initializing standard default settings.
     */
    public WizardData() {
    }

    /**
     * Gets the chosen database file path.
     *
     * @return file path to SQLite database
     */
    public String getDbPath() {
        return dbPath;
    }

    /**
     * Sets the database file path.
     *
     * @param dbPath destination path for SQLite file
     */
    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * Gets the selected visual theme.
     *
     * @return theme name (e.g. Light, Dark, System)
     */
    public String getTheme() {
        return theme;
    }

    /**
     * Sets the visual theme.
     *
     * @param theme chosen theme
     */
    public void setTheme(String theme) {
        this.theme = theme;
    }

    /**
     * Gets the interface language.
     *
     * @return selected language name
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the interface language.
     *
     * @param language chosen language
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Gets the lead time in minutes for task reminders.
     *
     * @return minutes before due date
     */
    public int getReminderMinutes() {
        return reminderMinutes;
    }

    /**
     * Sets the reminder lead time.
     *
     * @param reminderMinutes lead time in minutes
     */
    public void setReminderMinutes(int reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
    }

    /**
     * Checks if automatic periodic backups are enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isAutoBackup() {
        return autoBackup;
    }

    /**
     * Configures automatic periodic backup preference.
     *
     * @param autoBackup true to enable auto backups
     */
    public void setAutoBackup(boolean autoBackup) {
        this.autoBackup = autoBackup;
    }
}