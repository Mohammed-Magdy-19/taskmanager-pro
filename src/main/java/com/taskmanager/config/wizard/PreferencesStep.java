package com.taskmanager.config.wizard;

import com.taskmanager.view.components.CardPanel;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Wizard panel configuring visual theme, language, and background backup settings.
 * Uses CardPanel container styling and standardized typography.
 */
public class PreferencesStep extends CardPanel implements WizardStepPanel {

    private static final String[] THEMES = {"Light", "Dark", "System"};
    private static final String[] LANGUAGES = {"English"};

    private final JComboBox<String> themeComboBox;
    private final JComboBox<String> languageComboBox;
    private final JCheckBox autoBackupCheckBox;

    /**
     * Constructs the PreferencesStep panel initialized from {@link WizardData}.
     *
     * @param data existing configuration values
     */
    public PreferencesStep(WizardData data) {
        super(new BorderLayout(AppTheme.SPACING, AppTheme.SPACING));

        JLabel titleLabel = new JLabel("Application Preferences");
        titleLabel.setFont(AppTheme.FONT_HEADING);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(AppTheme.SPACING / 2, AppTheme.SPACING / 2, AppTheme.SPACING / 2, AppTheme.SPACING / 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel themeLabel = new JLabel("Interface Theme:");
        themeLabel.setFont(AppTheme.FONT_BODY_BOLD);
        themeLabel.setForeground(AppTheme.TEXT_PRIMARY);
        contentPanel.add(themeLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        themeComboBox = new JComboBox<>(THEMES);
        themeComboBox.setFont(AppTheme.FONT_BODY);
        if (data != null && data.getTheme() != null) {
            themeComboBox.setSelectedItem(data.getTheme());
        }
        contentPanel.add(themeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel langLabel = new JLabel("Language:");
        langLabel.setFont(AppTheme.FONT_BODY_BOLD);
        langLabel.setForeground(AppTheme.TEXT_PRIMARY);
        contentPanel.add(langLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        languageComboBox = new JComboBox<>(LANGUAGES);
        languageComboBox.setFont(AppTheme.FONT_BODY);
        if (data != null && data.getLanguage() != null) {
            languageComboBox.setSelectedItem(data.getLanguage());
        }
        contentPanel.add(languageComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        autoBackupCheckBox = new JCheckBox("Enable automatic periodic backups (Phase 6)");
        autoBackupCheckBox.setFont(AppTheme.FONT_BODY);
        autoBackupCheckBox.setForeground(AppTheme.TEXT_PRIMARY);
        autoBackupCheckBox.setOpaque(false);
        autoBackupCheckBox.setSelected(data == null || data.isAutoBackup());
        contentPanel.add(autoBackupCheckBox, gbc);

        add(contentPanel, BorderLayout.CENTER);
    }

    @Override
    public void commit(WizardData data) {
        if (data != null) {
            Object selectedTheme = themeComboBox.getSelectedItem();
            if (selectedTheme != null) {
                data.setTheme(selectedTheme.toString());
            }
            Object selectedLang = languageComboBox.getSelectedItem();
            if (selectedLang != null) {
                data.setLanguage(selectedLang.toString());
            }
            data.setAutoBackup(autoBackupCheckBox.isSelected());
        }
    }

    @Override
    public void refresh(WizardData data) {
        if (data != null) {
            if (data.getTheme() != null) {
                themeComboBox.setSelectedItem(data.getTheme());
            }
            if (data.getLanguage() != null) {
                languageComboBox.setSelectedItem(data.getLanguage());
            }
            autoBackupCheckBox.setSelected(data.isAutoBackup());
        }
    }
}
