package com.taskmanager.event;

import java.io.File;
import java.util.Date;
import java.util.Objects;

/**
 * Immutable event published upon the successful generation of a database backup file.
 * Carries metadata regarding the created archive file and the count of backed up entities.
 */
public final class BackupCompletedEvent {

    private final File backupFile;
    private final int taskCount;
    private final int userCount;
    private final Date timestamp;

    /**
     * Constructs a BackupCompletedEvent with the specified backup metadata.
     *
     * @param backupFile the created backup archive file
     * @param taskCount count of tasks backed up
     * @param userCount count of users backed up
     * @param timestamp timestamp when backup was created
     */
    public BackupCompletedEvent(File backupFile, int taskCount, int userCount, Date timestamp) {
        this.backupFile = Objects.requireNonNull(backupFile, "backupFile cannot be null");
        this.taskCount = taskCount;
        this.userCount = userCount;
        this.timestamp = (timestamp != null) ? new Date(timestamp.getTime()) : new Date();
    }

    /**
     * Retrieves the backup file that was created.
     *
     * @return the backup File
     */
    public File getBackupFile() {
        return backupFile;
    }

    /**
     * Retrieves the count of tasks backed up.
     *
     * @return task count
     */
    public int getTaskCount() {
        return taskCount;
    }

    /**
     * Retrieves the count of users backed up.
     *
     * @return user count
     */
    public int getUserCount() {
        return userCount;
    }

    /**
     * Retrieves the creation timestamp of the backup.
     *
     * @return copy of the timestamp Date
     */
    public Date getTimestamp() {
        return new Date(timestamp.getTime());
    }

    @Override
    public String toString() {
        return "BackupCompletedEvent{file=" + backupFile.getName() + ", tasks=" + taskCount + ", users=" + userCount + "}";
    }
}
