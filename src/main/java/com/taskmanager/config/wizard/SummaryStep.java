package com.taskmanager.config.wizard;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Final wizard panel displaying a read-only summary of chosen configuration parameters
 * before the user finalizes the setup.
 */
public class SummaryStep extends JPanel implements WizardStepPanel {

    private final JLabel dbPathValLabel = new JLabel();
    private final JLabel themeValLabel = new JLabel();
    private final JLabel languageValLabel = new JLabel();
    private final JLabel reminderValLabel = new JLabel();
    private final JLabel autoBackupValLabel = new JLabel();

    /**
     * Constructs the SummaryStep panel and initializes summary labels.
     *
     * @param data initial wizard configuration state
     */
    public SummaryStep(WizardData data) {
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel("Configuration Summary");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        addSummaryRow(contentPanel, gbc, 0, "Database File:", dbPathValLabel);
        addSummaryRow(contentPanel, gbc, 1, "Visual Theme:", themeValLabel);
        addSummaryRow(contentPanel, gbc, 2, "Language:", languageValLabel);
        addSummaryRow(contentPanel, gbc, 3, "Reminder Interval:", reminderValLabel);
        addSummaryRow(contentPanel, gbc, 4, "Auto Backup:", autoBackupValLabel);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 6, 6, 6);
        JLabel noteLabel = new JLabel("Click 'Finish' to save these settings and launch Task Manager.");
        noteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        contentPanel.add(noteLabel, gbc);

        add(contentPanel, BorderLayout.CENTER);

        if (data != null) {
            refresh(data);
        }
    }

    private void addSummaryRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JLabel valueLabel) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        JLabel headerLabel = new JLabel(labelText);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(headerLabel, gbc);

        gbc.gridx = 1;
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(valueLabel, gbc);
    }

    @Override
    public void commit(WizardData data) {
        // Summary step is read-only
    }

    @Override
    public void refresh(WizardData data) {
        if (data == null) {
            return;
        }
        dbPathValLabel.setText(data.getDbPath());
        themeValLabel.setText(data.getTheme());
        languageValLabel.setText(data.getLanguage());
        reminderValLabel.setText(data.getReminderMinutes() + " minutes before due date");
        autoBackupValLabel.setText(data.isAutoBackup() ? "Enabled" : "Disabled");
    }
}
