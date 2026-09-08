package com.taskmanager.service;

import com.taskmanager.config.ConfigManager;
import com.taskmanager.model.BackupData;
import com.taskmanager.model.Task;
import com.taskmanager.model.User;
import com.taskmanager.util.AppLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service managing database backup generation, available backup discovery, and restoration.
 * Keeps file I/O operations thread-agnostic so callers can submit requests asynchronously.
 */
public class BackupManager {

    private static final AppLogger LOGGER = AppLogger.getLogger(BackupManager.class);
    private static final String DEFAULT_BACKUP_DIR = "./backups";
    private static final String BACKUP_EXTENSION = ".ser";

    private final Path backupDirectory;
    private final SerializationUtil<BackupData> serializer;

    /**
     * Constructs a BackupManager reading the backup destination directory from {@link ConfigManager}.
     */
    public BackupManager() {
        this(ConfigManager.getInstance().getProperty("backup.path", DEFAULT_BACKUP_DIR));
    }

    /**
     * Constructs a BackupManager using the specified backup directory path.
     * Ensures the directory structure exists on the filesystem.
     *
     * @param backupDirectoryPath path to the directory where backup files are stored
     */
    public BackupManager(String backupDirectoryPath) {
        this(Paths.get(backupDirectoryPath != null ? backupDirectoryPath : DEFAULT_BACKUP_DIR));
    }

    /**
     * Constructs a BackupManager targeting the specified {@link Path}.
     *
     * @param backupDirectory target filesystem directory
     */
    public BackupManager(Path backupDirectory) {
        this.backupDirectory = Objects.requireNonNull(backupDirectory, "backupDirectory must not be null");
        this.serializer = new SerializationUtil<>();
        initDirectory();
    }

    private void initDirectory() {
        try {
            if (!Files.exists(backupDirectory)) {
                Files.createDirectories(backupDirectory);
                LOGGER.info("Created backup directory: " + backupDirectory.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to initialize backup directory: " + backupDirectory, e);
            throw new RuntimeException("Could not create backup directory: " + backupDirectory, e);
        }
    }

    /**
     * Creates a serialized backup archive containing the provided tasks and users.
     * Appends a random UUID suffix to prevent millisecond collision when invoked in rapid succession.
     *
     * @param tasks list of tasks to include in backup
     * @param users list of users to include in backup
     * @return the {@link File} reference of the newly created backup archive
     * @throws IOException if file serialization fails
     */
    public File createBackup(List<Task> tasks, List<User> users) throws IOException {
        initDirectory(); // Ensure directory exists if deleted externally

        BackupData data = new BackupData(tasks, users, new Date());
        String fileName = "backup_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + BACKUP_EXTENSION;
        File targetFile = backupDirectory.resolve(fileName).toFile();

        serializer.serialize(data, targetFile.getAbsolutePath());
        LOGGER.info("Backup successfully generated at: " + targetFile.getAbsolutePath()
                + " (Tasks: " + (tasks != null ? tasks.size() : 0) + ")");
        return targetFile;
    }

    /**
     * Deserializes and restores {@link BackupData} from the given file path.
     * Propagates serialization and class-compatibility exceptions to callers for error handling.
     *
     * @param filePath path to the serialized backup file
     * @return the deserialized {@link BackupData} container
     * @throws IOException if an I/O error occurs or the stream is corrupted
     * @throws ClassNotFoundException if an incompatible class version is encountered
     */
    public BackupData restoreBackup(String filePath) throws IOException, ClassNotFoundException {
        Objects.requireNonNull(filePath, "Backup file path cannot be null");
        LOGGER.info("Restoring backup from: " + filePath);
        return serializer.deserialize(filePath, BackupData.class);
    }

    /**
     * Retrieves all available backup files (*.ser) sorted by last-modified timestamp descending (newest first).
     *
     * @return sorted list of available backup files, or an empty list if none exist
     */
    public List<File> getAvailableBackups() {
        if (!Files.exists(backupDirectory)) {
            return Collections.emptyList();
        }

        File[] files = backupDirectory.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(BACKUP_EXTENSION));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        fileList.sort(Comparator.comparingLong(File::lastModified).reversed());
        return fileList;
    }

    /**
     * Retrieves the configured backup directory path.
     *
     * @return the backup directory Path
     */
    public Path getBackupDirectory() {
        return backupDirectory;
    }
}
