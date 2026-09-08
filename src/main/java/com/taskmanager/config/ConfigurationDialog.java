package com.taskmanager.config;

import com.taskmanager.config.wizard.DatabaseStep;
import com.taskmanager.config.wizard.PreferencesStep;
import com.taskmanager.config.wizard.ReminderStep;
import com.taskmanager.config.wizard.WizardData;
import com.taskmanager.config.wizard.WizardStepPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

/**
 * Reusable configuration dialog accessible from application menus to modify settings post-install.
 * Reuses the step panel components from the setup wizard within a {@link JTabbedPane}.
 */
public class ConfigurationDialog extends JDialog {

    private static final int DIALOG_WIDTH = 560;
    private static final int DIALOG_HEIGHT = 420;

    private final WizardData settingsData = new WizardData();
    private final List<WizardStepPanel> stepPanels = new ArrayList<>();

    /**
     * Constructs a modal configuration dialog bound to a parent window frame.
     *
     * @param owner parent frame, or null for top-level dialog
     */
    public ConfigurationDialog(Frame owner) {
        super(owner, "Task Manager — Settings", true);

        initWindow();
        loadExistingConfiguration();
        initTabs();
        initControls();
    }

    /**
     * Default constructor for headless testing or standalone invocation.
     */
    public ConfigurationDialog() {
        this(null);
    }

    private void initWindow() {
        setSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
        setResizable(false);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
    }

    private void loadExistingConfiguration() {
        ConfigManager config = ConfigManager.getInstance();
        settingsData.setDbPath(config.getProperty(ConfigManager.KEY_DB_PATH, "taskmanager.db"));
        settingsData.setTheme(config.getProperty(ConfigManager.KEY_APP_THEME, "Light"));
        settingsData.setLanguage(config.getProperty(ConfigManager.KEY_APP_LANGUAGE, "English"));
        settingsData.setReminderMinutes(config.getIntProperty(ConfigManager.KEY_REMINDER_MINUTES, 15));
        settingsData.setAutoBackup(config.getBooleanProperty(ConfigManager.KEY_AUTO_BACKUP, true));
    }

    private void initTabs() {
        JTabbedPane tabbedPane = new JTabbedPane();

        DatabaseStep dbStep = new DatabaseStep(settingsData);
        PreferencesStep prefStep = new PreferencesStep(settingsData);
        ReminderStep reminderStep = new ReminderStep(settingsData);

        stepPanels.add(dbStep);
        stepPanels.add(prefStep);
        stepPanels.add(reminderStep);

        tabbedPane.addTab("Database", dbStep);
        tabbedPane.addTab("Preferences", prefStep);
        tabbedPane.addTab("Reminders", reminderStep);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initControls() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        JButton saveButton = new JButton("Save Changes");
        saveButton.addActionListener(e -> onSave());
        buttonPanel.add(saveButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void onSave() {
        for (WizardStepPanel panel : stepPanels) {
            panel.commit(settingsData);
        }

        ConfigManager config = ConfigManager.getInstance();
        config.setProperty(ConfigManager.KEY_DB_PATH, settingsData.getDbPath());
        config.setProperty(ConfigManager.KEY_APP_THEME, settingsData.getTheme());
        config.setProperty(ConfigManager.KEY_APP_LANGUAGE, settingsData.getLanguage());
        config.setProperty(ConfigManager.KEY_REMINDER_MINUTES, String.valueOf(settingsData.getReminderMinutes()));
        config.setProperty(ConfigManager.KEY_AUTO_BACKUP, String.valueOf(settingsData.isAutoBackup()));

        dispose();
    }

    /**
     * Package-private getter exposing loaded settings data for validation and testing.
     *
     * @return current dialog settings data
     */
    WizardData getSettingsData() {
        return settingsData;
    }
}
