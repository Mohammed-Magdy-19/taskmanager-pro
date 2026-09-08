package com.taskmanager.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized application logger wrapping {@link java.util.logging.Logger}.
 * Provides consistent logging across all layers and avoids unformatted console output.
 */
public final class AppLogger {

    private final Logger logger;

    private AppLogger(Class<?> clazz) {
        this.logger = Logger.getLogger(clazz.getName());
    }

    /**
     * Creates or retrieves a logger instance for the specified class.
     *
     * @param clazz the class requesting logging services
     * @return an {@link AppLogger} bound to the given class name
     */
    public static AppLogger getLogger(Class<?> clazz) {
        return new AppLogger(clazz);
    }

    /**
     * Logs an informational message.
     *
     * @param message the message to record
     */
    public void info(String message) {
        logger.log(Level.INFO, message);
    }

    /**
     * Logs a warning message when a non-fatal anomaly occurs.
     *
     * @param message the warning details
     */
    public void warn(String message) {
        logger.log(Level.WARNING, message);
    }

    /**
     * Logs an error message and associated exception.
     *
     * @param message description of the failure
     * @param throwable the cause of the failure
     */
    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
