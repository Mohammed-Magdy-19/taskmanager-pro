package com.taskmanager;

import com.taskmanager.config.ConfigManager;
import com.taskmanager.config.ConfigWizardFrame;
import com.taskmanager.util.AppLogger;
import com.taskmanager.view.theme.AppTheme;

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
     * Instantiates {@link com.taskmanager.view.MainFrame} and initializes
     * {@link com.taskmanager.service.NotificationServiceImpl}.
     */
    public static void launchMainApp() {
        LOGGER.info("Launching MainFrame.");
        try {
            com.taskmanager.view.MainFrame mainFrame = new com.taskmanager.view.MainFrame();
            mainFrame.setVisible(true);
            new com.taskmanager.service.NotificationServiceImpl();
        } catch (Exception e) {
            LOGGER.error("Fatal error during application launch: " + e.getMessage(), e);
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Failed to start Task Manager: " + e.getMessage(),
                    "Startup Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }
}