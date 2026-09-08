package com.taskmanager;

import com.taskmanager.config.ConfigManager;
import com.taskmanager.config.ConfigWizardFrame;
import com.taskmanager.util.AppLogger;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Main entry point of the Task Manager desktop application.
 * Initializes the visual theme and evaluates whether initial setup is required.
 */
public class MainApp {

    private static final AppLogger LOGGER = AppLogger.getLogger(MainApp.class);

    /**
     * Application start method on the Event Dispatch Thread (EDT).
     *
     * @param args command line parameters
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String theme = ConfigManager.getInstance().getProperty(ConfigManager.KEY_APP_THEME, "Light");
            AppTheme.setupLookAndFeel(theme);

            if (!ConfigManager.configExists()) {
                LOGGER.info("No existing configuration found. Launching ConfigWizardFrame.");
                new ConfigWizardFrame(MainApp::launchMainApp).setVisible(true);
            } else {
                LOGGER.info("Existing configuration detected. Proceeding to launchMainApp.");
                launchMainApp();
            }
        });
    }

    /**
     * Launches the main workspace window once configuration is established.
     * In Phase 4, this instantiates {@code com.taskmanager.view.MainFrame}.
     */
    public static void launchMainApp() {
        System.out.println("Main app would launch here");
        JFrame frame = new JFrame("Task Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        JLabel placeholderLabel = new JLabel("Main app would launch here (Phase 4)", SwingConstants.CENTER);
        placeholderLabel.setFont(AppTheme.FONT_HEADING);
        frame.add(placeholderLabel);
        frame.setVisible(true);
    }
}