package com.taskmanager.service;

import com.taskmanager.model.BackupData;
import com.taskmanager.model.Task;
import com.taskmanager.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupManagerTest {

    private Path tempBackupDir;
    private BackupManager backupManager;

    @BeforeEach
    void setUp() throws IOException {
        tempBackupDir = Files.createTempDirectory("backup_manager_test_");
        backupManager = new BackupManager(tempBackupDir);
    }

    @AfterEach
    void tearDown() throws IOException {
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
    @DisplayName("createBackup() produces exactly one new .ser file in backup dir and getAvailableBackups() reflects it")
    void testCreateBackupProducesSingleFile() throws Exception {
        Task task = new Task();
        task.setTitle("Backup Target Task");

        File backupFile = backupManager.createBackup(List.of(task), Collections.emptyList());

        assertNotNull(backupFile);
        assertTrue(backupFile.exists(), "Backup file must exist on filesystem");
        assertTrue(backupFile.getName().endsWith(".ser"), "Backup file must have .ser extension");

        List<File> available = backupManager.getAvailableBackups();
        assertEquals(1, available.size(), "Should have exactly one available backup");
        assertEquals(backupFile.getAbsolutePath(), available.get(0).getAbsolutePath());
    }

    @Test
    @DisplayName("Rapid successive createBackup() calls produce TWO distinct files without millisecond collision")
    void testRapidSuccessiveCreateBackupProducesTwoFiles() throws Exception {
        Task t1 = new Task();
        t1.setTitle("Rapid 1");
        Task t2 = new Task();
        t2.setTitle("Rapid 2");

        File file1 = backupManager.createBackup(List.of(t1), Collections.emptyList());
        File file2 = backupManager.createBackup(List.of(t2), Collections.emptyList());

        assertNotEquals(file1.getName(), file2.getName(), "Filenames must be distinct even when created rapidly");
        assertTrue(file1.exists());
        assertTrue(file2.exists());

        List<File> available = backupManager.getAvailableBackups();
        assertEquals(2, available.size(), "Must have two distinct backup archives");
    }

    @Test
    @DisplayName("restoreBackup() returns BackupData with exact matching task and user counts")
    void testRestoreBackupPreservesCounts() throws Exception {
        Task t1 = new Task();
        t1.setId(1);
        t1.setTitle("Task 1");

        Task t2 = new Task();
        t2.setId(2);
        t2.setTitle("Task 2");

        User u1 = new User("admin", "admin@taskmanager.com", "Admin User");

        File backupFile = backupManager.createBackup(List.of(t1, t2), List.of(u1));
        BackupData restored = backupManager.restoreBackup(backupFile.getAbsolutePath());

        assertNotNull(restored);
        assertEquals(2, restored.getTasks().size());
        assertEquals("Task 1", restored.getTasks().get(0).getTitle());
        assertEquals("Task 2", restored.getTasks().get(1).getTitle());
        assertEquals(1, restored.getUsers().size());
        assertEquals("admin", restored.getUsers().get(0).getUsername());
    }

    @Test
    @DisplayName("restoreBackup() on corrupted file throws exception rather than returning null or partial object")
    void testRestoreBackupCorruptedThrows() throws Exception {
        File corruptedFile = tempBackupDir.resolve("corrupted_backup.ser").toFile();
        try (FileOutputStream fos = new FileOutputStream(corruptedFile)) {
            fos.write("CORRUPT HEADER AND PAYLOAD DATA".getBytes(StandardCharsets.UTF_8));
        }

        assertThrows(Exception.class, () -> {
            backupManager.restoreBackup(corruptedFile.getAbsolutePath());
        });
    }

    @Test
    @DisplayName("getAvailableBackups() on an empty directory returns empty list, not null or exception")
    void testGetAvailableBackupsEmptyDirReturnsEmptyList() {
        List<File> backups = backupManager.getAvailableBackups();
        assertNotNull(backups);
        assertTrue(backups.isEmpty());
    }

    @Test
    @DisplayName("getAvailableBackups() correctly sorts multiple backups newest-first (descending last-modified)")
    void testGetAvailableBackupsSortsNewestFirst() throws Exception {
        File f1 = tempBackupDir.resolve("backup_old.ser").toFile();
        File f2 = tempBackupDir.resolve("backup_mid.ser").toFile();
        File f3 = tempBackupDir.resolve("backup_new.ser").toFile();

        assertTrue(f1.createNewFile());
        assertTrue(f2.createNewFile());
        assertTrue(f3.createNewFile());

        // Set explicit timestamps: f1 oldest, f2 middle, f3 newest
        long baseTime = 1_000_000_000L;
        assertTrue(f1.setLastModified(baseTime));
        assertTrue(f2.setLastModified(baseTime + 10_000L));
        assertTrue(f3.setLastModified(baseTime + 20_000L));

        List<File> sortedBackups = backupManager.getAvailableBackups();
        assertEquals(3, sortedBackups.size());
        assertEquals(f3.getName(), sortedBackups.get(0).getName(), "Newest file must be first");
        assertEquals(f2.getName(), sortedBackups.get(1).getName(), "Middle file must be second");
        assertEquals(f1.getName(), sortedBackups.get(2).getName(), "Oldest file must be third");
    }

    @Test
    @DisplayName("Constructing BackupManager creates missing directory, and does not throw if directory already exists")
    void testDirectoryCreationAndIdempotence() throws Exception {
        Path newDir = tempBackupDir.resolve("sub_backup_dir");
        assertFalse(Files.exists(newDir));

        BackupManager bm1 = new BackupManager(newDir);
        assertTrue(Files.exists(newDir), "Directory should be created upon initialization");

        assertDoesNotThrow(() -> {
            new BackupManager(newDir);
        }, "Reconstructing BackupManager with existing directory must not throw");
    }
}
