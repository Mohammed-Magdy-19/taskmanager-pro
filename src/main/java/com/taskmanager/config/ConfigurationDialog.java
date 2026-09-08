package com.taskmanager.config;

import com.taskmanager.config.wizard.DatabaseStep;
import com.taskmanager.config.wizard.PreferencesStep;
import com.taskmanager.config.wizard.ReminderStep;
import com.taskmanager.config.wizard.WizardData;
import com.taskmanager.config.wizard.WizardStepPanel;
import com.taskmanager.controller.SettingsController;
import com.taskmanager.event.BackupCompletedEvent;
import com.taskmanager.event.EventBus;
import com.taskmanager.view.components.CardPanel;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
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

    private static final int DIALOG_WIDTH = 580;
    private static final int DIALOG_HEIGHT = 440;

    private final WizardData settingsData = new WizardData();
    private final List<WizardStepPanel> stepPanels = new ArrayList<>();
    private final SettingsController settingsController;

    /**
     * Constructs a modal configuration dialog bound to a parent window frame.
     *
     * @param owner parent frame, or null for top-level dialog
     */
    public ConfigurationDialog(Frame owner) {
        super(owner, "Task Manager — Settings", true);
        this.settingsController = new SettingsController();

        initWindow();
        loadExistingConfiguration();
        initTabs();
        initControls();
        initEventSubscription();
    }

    private void initEventSubscription() {
        EventBus.getInstance().subscribe(BackupCompletedEvent.class, event -> {
            if (isVisible()) {
                JOptionPane.showMessageDialog(this,
                        "Backup archive generated successfully!\nFile: " + event.getBackupFile().getName()
                                + "\nTasks backed up: " + event.getTaskCount(),
                        "Backup Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
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
        getContentPane().setBackground(AppTheme.BG_APP);
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
        tabbedPane.setFont(AppTheme.FONT_BUTTON);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACING, AppTheme.SPACING, 0, AppTheme.SPACING));

        DatabaseStep dbStep = new DatabaseStep(settingsData);
        PreferencesStep prefStep = new PreferencesStep(settingsData);
        ReminderStep reminderStep = new ReminderStep(settingsData);

        stepPanels.add(dbStep);
        stepPanels.add(prefStep);
        stepPanels.add(reminderStep);

        tabbedPane.addTab("Database", dbStep);
        tabbedPane.addTab("Preferences", prefStep);
        tabbedPane.addTab("Reminders", reminderStep);
        tabbedPane.addTab("Backup", createBackupTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createBackupTab() {
        CardPanel card = new CardPanel(new BorderLayout(0, AppTheme.SPACING));
        card.setBorder(BorderFactory.createEmptyBorder(AppTheme.SPACING, AppTheme.SPACING, AppTheme.SPACING, AppTheme.SPACING));

        JPanel contentBox = new JPanel();
        contentBox.setLayout(new javax.swing.BoxLayout(contentBox, javax.swing.BoxLayout.Y_AXIS));
        contentBox.setOpaque(false);

        javax.swing.JLabel backupTitle = new javax.swing.JLabel("Database Backup & Disaster Recovery");
        backupTitle.setFont(AppTheme.FONT_SUBHEADING);
        backupTitle.setForeground(AppTheme.TEXT_PRIMARY);

        javax.swing.JLabel backupDesc = new javax.swing.JLabel("Export tasks and user records to a serialized backup archive (.ser), or restore from an existing archive.");
        backupDesc.setFont(AppTheme.FONT_BODY);
        backupDesc.setForeground(AppTheme.TEXT_MUTED);

        contentBox.add(backupTitle);
        contentBox.add(javax.swing.Box.createVerticalStrut(6));
        contentBox.add(backupDesc);
        contentBox.add(javax.swing.Box.createVerticalStrut(AppTheme.SPACING * 2));

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.SPACING, 0));
        actionsPanel.setOpaque(false);

        JButton backupNowButton = new JButton("📦 Backup Now");
        backupNowButton.setFont(AppTheme.FONT_BUTTON);
        backupNowButton.setBackground(AppTheme.PRIMARY);
        backupNowButton.setForeground(AppTheme.TEXT_INVERTED);
        backupNowButton.addActionListener(e -> settingsController.createBackupAsync(this));

        JButton restoreButton = new JButton("🔄 Restore from Backup...");
        restoreButton.setFont(AppTheme.FONT_BUTTON);
        restoreButton.addActionListener(e -> settingsController.restoreFromBackupAsync(this));

        actionsPanel.add(backupNowButton);
        actionsPanel.add(restoreButton);

        contentBox.add(actionsPanel);
        contentBox.add(javax.swing.Box.createVerticalGlue());

        card.add(contentBox, BorderLayout.CENTER);
        return card;
    }

    private void initControls() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.SPACING, AppTheme.SPACING));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, AppTheme.SPACING, AppTheme.SPACING, AppTheme.SPACING));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(AppTheme.FONT_BUTTON);
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        JButton saveButton = new JButton("Save Changes");
        saveButton.setFont(AppTheme.FONT_BUTTON);
        saveButton.setBackground(AppTheme.PRIMARY);
        saveButton.setForeground(AppTheme.TEXT_INVERTED);
        saveButton.putClientProperty("JButton.buttonType", "roundRect");
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
