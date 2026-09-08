package com.taskmanager.config.wizard;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Wizard panel configuring reminder lead times.
 * Controls how many minutes before a task's due date a notification event will fire.
 */
public class ReminderStep extends JPanel implements WizardStepPanel {

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
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel("Notification & Reminder Settings");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        contentPanel.add(new JLabel("Remind me before due date (minutes):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        int initial = (data != null && data.getReminderMinutes() >= MIN_MINUTES)
                ? data.getReminderMinutes()
                : DEFAULT_MINUTES;
        SpinnerNumberModel model = new SpinnerNumberModel(initial, MIN_MINUTES, MAX_MINUTES, STEP_SIZE);
        minutesSpinner = new JSpinner(model);
        contentPanel.add(minutesSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        JLabel helpLabel = new JLabel("The background scheduler will display alerts before approaching deadlines.");
        helpLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
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
