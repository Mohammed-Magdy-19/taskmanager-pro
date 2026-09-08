package com.taskmanager.config.wizard;

import com.taskmanager.view.components.CardPanel;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Wizard panel configuring reminder lead times.
 * Uses CardPanel container styling and standardized typography.
 */
public class ReminderStep extends CardPanel implements WizardStepPanel {

    private static final int MIN_MINUTES = 1;
    private static final int MAX_MINUTES = 1440; // 24 hours
    private static final int STEP_SIZE = 5;
    private static final int DEFAULT_MINUTES = 15;

    private final JSpinner minutesSpinner;

    /**
     * Constructs the ReminderStep panel with a numeric spinner.
     *
     * @param data initial wizard configuration
     */
    public ReminderStep(WizardData data) {
        super(new BorderLayout(AppTheme.SPACING, AppTheme.SPACING));

        JLabel titleLabel = new JLabel("Notification & Reminder Settings");
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
        JLabel leadTimeLabel = new JLabel("Remind before deadline (minutes):");
        leadTimeLabel.setFont(AppTheme.FONT_BODY_BOLD);
        leadTimeLabel.setForeground(AppTheme.TEXT_PRIMARY);
        contentPanel.add(leadTimeLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        int initial = (data != null && data.getReminderMinutes() >= MIN_MINUTES)
                ? data.getReminderMinutes()
                : DEFAULT_MINUTES;
        SpinnerNumberModel model = new SpinnerNumberModel(initial, MIN_MINUTES, MAX_MINUTES, STEP_SIZE);
        minutesSpinner = new JSpinner(model);
        minutesSpinner.setFont(AppTheme.FONT_BODY);
        contentPanel.add(minutesSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        JLabel helpLabel = new JLabel("The background scheduler will display alerts before approaching deadlines.");
        helpLabel.setFont(AppTheme.FONT_CAPTION);
        helpLabel.setForeground(AppTheme.TEXT_MUTED);
        contentPanel.add(helpLabel, gbc);

        add(contentPanel, BorderLayout.CENTER);
    }

    @Override
    public void commit(WizardData data) {
        if (data != null) {
            Object value = minutesSpinner.getValue();
            if (value instanceof Number number) {
                data.setReminderMinutes(number.intValue());
            }
        }
    }

    @Override
    public void refresh(WizardData data) {
        if (data != null) {
            minutesSpinner.setValue(data.getReminderMinutes());
        }
    }
}
