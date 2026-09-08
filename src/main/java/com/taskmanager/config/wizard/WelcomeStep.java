package com.taskmanager.config.wizard;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Initial introductory panel of the configuration wizard.
 * Presents a welcome message and outlines the configuration setup process.
 */
public class WelcomeStep extends JPanel implements WizardStepPanel {

    private static final String TITLE_TEXT = "Welcome to Task Manager";
    private static final String BODY_TEXT =
            "This setup wizard will help you configure essential settings before launching Task Manager:\n\n"
            + "  • Database storage location\n"
            + "  • Visual theme and language preferences\n"
            + "  • Task notification and reminder intervals\n\n"
            + "Click 'Next' to begin configuration.";

    /**
     * Constructs the WelcomeStep panel with descriptive introductory text.
     */
    public WelcomeStep() {
        setLayout(new BorderLayout(16, 16));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel(TITLE_TEXT, SwingConstants.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        JTextArea bodyArea = new JTextArea(BODY_TEXT);
        bodyArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bodyArea.setEditable(false);
        bodyArea.setFocusable(false);
        bodyArea.setOpaque(false);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        add(bodyArea, BorderLayout.CENTER);
    }

    @Override
    public void commit(WizardData data) {
        // Welcome step contains no user-editable inputs
    }
}
