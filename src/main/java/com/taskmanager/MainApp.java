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
     * Instantiates {@link com.taskmanager.view.MainFrame}.
     */
    public static void launchMainApp() {
        LOGGER.info("Launching MainFrame.");
        new com.taskmanager.view.MainFrame().setVisible(true);
    }
}