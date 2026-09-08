package com.taskmanager.config.wizard;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Wizard panel configuring visual theme, language, and background backup settings.
 */
public class PreferencesStep extends JPanel implements WizardStepPanel {

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
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel("Application Preferences");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        contentPanel.add(new JLabel("Theme:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        themeComboBox = new JComboBox<>(THEMES);
        if (data != null && data.getTheme() != null) {
            themeComboBox.setSelectedItem(data.getTheme());
        }
        contentPanel.add(themeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        contentPanel.add(new JLabel("Language:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        languageComboBox = new JComboBox<>(LANGUAGES);
        if (data != null && data.getLanguage() != null) {
            languageComboBox.setSelectedItem(data.getLanguage());
        }
        contentPanel.add(languageComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        autoBackupCheckBox = new JCheckBox("Enable automatic periodic backups (Phase 6)");
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
