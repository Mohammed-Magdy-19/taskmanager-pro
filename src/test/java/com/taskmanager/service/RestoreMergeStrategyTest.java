package com.taskmanager.service;

import com.taskmanager.config.ConfigManager;
import com.taskmanager.controller.SettingsController;
import com.taskmanager.model.BackupData;
import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestoreMergeStrategyTest {

    private File tempDbFile;
    private Path tempBackupDir;
    private com.taskmanager.repository.DatabaseConnection dbConnection;
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private BackupManager backupManager;
    private SettingsController settingsController;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = Files.createTempFile("restore_merge_test_", ".db").toFile();
        tempBackupDir = Files.createTempDirectory("restore_merge_backup_");
        ConfigManager.getInstance().setProperty(ConfigManager.KEY_DB_PATH, tempDbFile.getAbsolutePath());
        dbConnection = com.taskmanager.repository.DatabaseConnection.resetInstance(tempDbFile.getAbsolutePath());

        taskRepository = new TaskRepository(dbConnection);
        userRepository = new UserRepository(dbConnection);
        backupManager = new BackupManager(tempBackupDir);
        settingsController = new SettingsController(backupManager, taskRepository, userRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dbConnection != null) {
            dbConnection.close();
        }
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
        if (tempBackupDir != null && Files.exists(tempBackupDir)) {
            File[] files = tempBackupDir.toFile().listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            Files.deleteIfExists(tempBackupDir);
        }
    }

    @Test
    @DisplayName("Restoring backup into an EMPTY database inserts all tasks with original IDs preserved")
    void testRestoreIntoEmptyDatabasePreservesOriginalIds() throws Exception {
        // Create backup data with fixed IDs
        Task t1 = new Task();
        t1.setId(101);
        t1.setTitle("Preserved Task 101");
        t1.setPriority(Priority.HIGH);
        t1.setStatus(Status.PENDING);
        t1.setCreatedAt(LocalDateTime.now().minusDays(1));

        Task t2 = new Task();
        t2.setId(202);
        t2.setTitle("Preserved Task 202");
        t2.setPriority(Priority.LOW);
        t2.setStatus(Status.COMPLETED);
        t2.setCreatedAt(LocalDateTime.now().minusDays(2));

        File backupFile = backupManager.createBackup(List.of(t1, t2), Collections.emptyList());

        // Ensure database is completely empty before restore
        assertEquals(0, taskRepository.count(), "DB must start empty");

        // Restore backup archive
        BackupData restoredData = backupManager.restoreBackup(backupFile.getAbsolutePath());
        int mergedCount = settingsController.applyMergeStrategy(restoredData);

        assertEquals(2, mergedCount);
        assertEquals(2, taskRepository.count(), "DB must contain 2 tasks");

        Task fromDb1 = taskRepository.findById(101).orElse(null);
        assertNotNull(fromDb1, "Task 101 must exist in DB");
        assertEquals(101, fromDb1.getId(), "Original ID 101 must be preserved");
        assertEquals("Preserved Task 101", fromDb1.getTitle());
        assertEquals(Priority.HIGH, fromDb1.getPriority());

        Task fromDb2 = taskRepository.findById(202).orElse(null);
        assertNotNull(fromDb2, "Task 202 must exist in DB");
        assertEquals(202, fromDb2.getId(), "Original ID 202 must be preserved");
        assertEquals("Preserved Task 202", fromDb2.getTitle());
        assertEquals(Priority.LOW, fromDb2.getPriority());
    }

    @Test
    @DisplayName("Restoring backup into a database with OVERLAPPING IDs upserts correctly without duplicates")
    void testRestoreWithOverlappingIdsUpsertsWithoutDuplicates() throws Exception {
        // 1. Insert initial tasks into database
        Task existing1 = new Task();
        existing1.setTitle("Old Title 1");
        existing1.setStatus(Status.PENDING);
        taskRepository.save(existing1);
        int id1 = existing1.getId();

        Task existing2 = new Task();
        existing2.setTitle("Unmodified Task 2");
        existing2.setStatus(Status.PENDING);
        taskRepository.save(existing2);
        int id2 = existing2.getId();

        assertEquals(2, taskRepository.count());

        // 2. Prepare backup with overlapping ID for task 1, and a brand new task 3
        Task backupTask1 = new Task();
        backupTask1.setId(id1);
        backupTask1.setTitle("Overwritten Title from Backup");
        backupTask1.setStatus(Status.COMPLETED);

        Task backupTask3 = new Task();
        backupTask3.setId(999);
        backupTask3.setTitle("Brand New Task 999");
        backupTask3.setStatus(Status.IN_PROGRESS);

        File backupArchive = backupManager.createBackup(List.of(backupTask1, backupTask3), Collections.emptyList());

        // 3. Restore and merge
        BackupData restoredData = backupManager.restoreBackup(backupArchive.getAbsolutePath());
        int mergedCount = settingsController.applyMergeStrategy(restoredData);

        assertEquals(2, mergedCount);
        // Total tasks in DB should be exactly 3: task 1 (updated), task 2 (kept untouched), task 3 (inserted)
        assertEquals(3, taskRepository.count(), "Row count must be 3; no duplicate row for ID " + id1);

        // Verify task 1 is overwritten
        Task updatedTask1 = taskRepository.findById(id1).orElseThrow();
        assertEquals("Overwritten Title from Backup", updatedTask1.getTitle());
        assertEquals(Status.COMPLETED, updatedTask1.getStatus());

        // Verify task 2 is untouched
        Task untouchedTask2 = taskRepository.findById(id2).orElseThrow();
        assertEquals("Unmodified Task 2", untouchedTask2.getTitle());
        assertEquals(Status.PENDING, untouchedTask2.getStatus());

        // Verify task 3 is inserted
        Task newTask3 = taskRepository.findById(999).orElseThrow();
        assertEquals("Brand New Task 999", newTask3.getTitle());
        assertEquals(Status.IN_PROGRESS, newTask3.getStatus());
    }

    @Test
    @DisplayName("Restoring an EMPTY BackupData into a populated DB does NOT delete or wipe existing data")
    void testRestoreEmptyBackupDoesNotWipeExistingData() throws Exception {
        // Pre-populate database with tasks
        Task t1 = new Task();
        t1.setTitle("Existing Data 1");
        taskRepository.save(t1);

        Task t2 = new Task();
        t2.setTitle("Existing Data 2");
        taskRepository.save(t2);

        assertEquals(2, taskRepository.count());

        // Create empty backup archive
        File emptyArchive = backupManager.createBackup(Collections.emptyList(), Collections.emptyList());

        // Restore empty backup
        BackupData restoredData = backupManager.restoreBackup(emptyArchive.getAbsolutePath());
        int mergedCount = settingsController.applyMergeStrategy(restoredData);

        assertEquals(0, mergedCount);
        // REPLACE strategy means "upsert what is in archive", NEVER wipe or truncate pre-existing records!
        assertEquals(2, taskRepository.count(), "Existing tasks must NOT be deleted when restoring empty backup");
        assertTrue(taskRepository.existsById(t1.getId()));
        assertTrue(taskRepository.existsById(t2.getId()));
    }
}
