package com.taskmanager.controller;

import com.taskmanager.concurrency.ThreadPoolManager;
import com.taskmanager.event.BackupCompletedEvent;
import com.taskmanager.event.EventBus;
import com.taskmanager.event.TaskProcessedEvent;
import com.taskmanager.model.BackupData;
import com.taskmanager.model.Task;
import com.taskmanager.model.User;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.service.BackupManager;
import com.taskmanager.util.AppLogger;
import com.taskmanager.util.SwingSafe;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Controller mediating configuration and data lifecycle actions including
 * asynchronous database backup and archive restoration.
 * Adheres to the one-way dependency rule and executes long-running file I/O off the EDT.
 */
public class SettingsController {

    private static final AppLogger LOGGER = AppLogger.getLogger(SettingsController.class);

    private final BackupManager backupManager;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    /**
     * Constructs a default SettingsController initialized with standard services and repositories.
     */
    public SettingsController() {
        this(new BackupManager(), new TaskRepository(), new UserRepository());
    }

    /**
     * Constructs a SettingsController with explicit dependencies for testing or custom configuration.
     *
     * @param backupManager the backup management service
     * @param taskRepository the task data repository
     * @param userRepository the user data repository
     */
    public SettingsController(BackupManager backupManager, TaskRepository taskRepository, UserRepository userRepository) {
        this.backupManager = Objects.requireNonNull(backupManager, "backupManager must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    /**
     * Initiates asynchronous backup generation on {@link ThreadPoolManager}.
     * Dispatches {@link BackupCompletedEvent} on success and presents user feedback.
     *
     * @param parentComponent the UI component context for dialog notifications
     */
    public void createBackupAsync(Component parentComponent) {
        ThreadPoolManager.getInstance().submit(() -> {
            try {
                List<Task> tasks = taskRepository.findAll();
                List<User> users = userRepository.findAll();
                File backupFile = backupManager.createBackup(tasks, users);

                EventBus.getInstance().publish(
                        new BackupCompletedEvent(backupFile, tasks.size(), users.size(), new Date())
                );
            } catch (Exception ex) {
                LOGGER.error("Failed to generate backup archive", ex);
                SwingSafe.run(() -> JOptionPane.showMessageDialog(
                        parentComponent,
                        "Failed to create backup: " + ex.getMessage(),
                        "Backup Error",
                        JOptionPane.ERROR_MESSAGE
                ));
            }
        });
    }

    /**
     * Prompts the user to choose a backup file (*.ser) and initiates asynchronous restoration
     * on {@link ThreadPoolManager} with explicit exception segregation for corruption and version mismatch.
     *
     * @param parentComponent the UI component context for dialogs
     */
    public void restoreFromBackupAsync(Component parentComponent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Task Manager Backup Archive");
        chooser.setFileFilter(new FileNameExtensionFilter("Task Manager Backup (*.ser)", "ser"));

        File defaultDir = backupManager.getBackupDirectory().toFile();
        if (defaultDir.exists()) {
            chooser.setCurrentDirectory(defaultDir);
        }

        int userSelection = chooser.showOpenDialog(parentComponent);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        if (selectedFile == null || !selectedFile.exists()) {
            JOptionPane.showMessageDialog(parentComponent, "Selected file does not exist.", "Invalid Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                BackupData backupData = backupManager.restoreBackup(selectedFile.getAbsolutePath());
                if (backupData == null) {
                    SwingSafe.run(() -> JOptionPane.showMessageDialog(
                            parentComponent,
                            "The backup file could not be read or contains no data.",
                            "Restore Failed",
                            JOptionPane.ERROR_MESSAGE
                    ));
                    return;
                }

                int restoredCount = applyMergeStrategy(backupData);
                SwingSafe.run(() -> JOptionPane.showMessageDialog(
                        parentComponent,
                        "System restored successfully!\nRestored " + restoredCount + " tasks from: " + selectedFile.getName(),
                        "Restore Complete",
                        JOptionPane.INFORMATION_MESSAGE
                ));

            } catch (InvalidClassException | ClassNotFoundException ex) {
                // Category 1: Serialization class/version incompatibility
                LOGGER.error("Incompatible backup class structure: " + selectedFile.getName(), ex);
                SwingSafe.run(() -> JOptionPane.showMessageDialog(
                        parentComponent,
                        "This backup archive is from an incompatible version of Task Manager.\n" + ex.getMessage(),
                        "Incompatible Backup Archive",
                        JOptionPane.ERROR_MESSAGE
                ));
            } catch (IOException ex) {
                // Category 2: Corrupted, malformed, or unreadable serialized file
                LOGGER.error("Corrupted or unreadable backup file: " + selectedFile.getName(), ex);
                SwingSafe.run(() -> JOptionPane.showMessageDialog(
                        parentComponent,
                        "The selected file is corrupted or not a valid Task Manager archive.\n" + ex.getMessage(),
                        "Corrupted Backup Archive",
                        JOptionPane.ERROR_MESSAGE
                ));
            } catch (Exception ex) {
                // General unexpected errors
                LOGGER.error("Unexpected error during archive restoration", ex);
                SwingSafe.run(() -> JOptionPane.showMessageDialog(
                        parentComponent,
                        "An unexpected error occurred during restore: " + ex.getMessage(),
                        "Restore Failed",
                        JOptionPane.ERROR_MESSAGE
                ));
            }
        });
    }

    /**
     * Applies the REPLACE merge strategy: upserts all tasks from the backup by their existing IDs.
     * Existing records with matching IDs are overwritten; new records are inserted with their original IDs.
     * Does NOT wipe or clear existing records that are not in the backup archive.
     *
     * @param data the restored backup container
     * @return the count of tasks merged into the database
     */
    public int applyMergeStrategy(BackupData data) {
        if (data == null) {
            return 0;
        }

        int count = 0;
        if (data.getTasks() != null) {
            for (Task task : data.getTasks()) {
                taskRepository.save(task);
                count++;
                EventBus.getInstance().publish(new TaskProcessedEvent(task));
            }
        }

        if (data.getUsers() != null) {
            for (User user : data.getUsers()) {
                userRepository.save(user);
            }
        }

        return count;
    }
}
