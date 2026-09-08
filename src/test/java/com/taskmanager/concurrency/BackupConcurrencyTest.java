package com.taskmanager.concurrency;

import com.taskmanager.config.ConfigManager;
import com.taskmanager.event.BackupCompletedEvent;
import com.taskmanager.event.EventBus;
import com.taskmanager.model.BackupData;
import com.taskmanager.model.Task;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.service.BackupManager;
import com.taskmanager.service.TaskServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupConcurrencyTest {

    private File tempDbFile;
    private Path tempBackupDir;
    private com.taskmanager.repository.DatabaseConnection dbConnection;
    private TaskRepository repository;
    private TaskServiceImpl service;
    private BackupManager backupManager;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = Files.createTempFile("backup_concurrency_test_", ".db").toFile();
        tempBackupDir = Files.createTempDirectory("backup_concurrency_dir_");
        ConfigManager.getInstance().setProperty(ConfigManager.KEY_DB_PATH, tempDbFile.getAbsolutePath());
        dbConnection = com.taskmanager.repository.DatabaseConnection.resetInstance(tempDbFile.getAbsolutePath());

        repository = new TaskRepository(dbConnection);
        service = new TaskServiceImpl(repository);
        backupManager = new BackupManager(tempBackupDir);
        EventBus.getInstance().clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        EventBus.getInstance().clear();
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
    @DisplayName("Simultaneous createBackup() and concurrent CRUD operations execute without exception or corruption")
    void testSimultaneousBackupAndConcurrentCrud() throws Throwable {
        final int concurrentCrudOperations = 25;
        final AtomicReference<Throwable> uncaughtException = new AtomicReference<>();
        final AtomicReference<File> backupFileRef = new AtomicReference<>();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(concurrentCrudOperations + 1);

        ExecutorService crudPool = Executors.newFixedThreadPool(6);

        try {
            // Concurrent CRUD threads
            for (int i = 0; i < concurrentCrudOperations; i++) {
                final int idx = i;
                crudPool.submit(() -> {
                    try {
                        startLatch.await();
                        Task task = new Task();
                        task.setTitle("Concurrent Task " + idx);
                        Task created = service.create(task);

                        created.setTitle(created.getTitle() + " [UPDATED]");
                        service.update(created);
                    } catch (Throwable t) {
                        uncaughtException.compareAndSet(null, t);
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            // Backup generation submitted via ThreadPoolManager
            ThreadPoolManager.getInstance().submit(() -> {
                try {
                    startLatch.await();
                    List<Task> tasks = repository.findAll();
                    File file = backupManager.createBackup(tasks, Collections.emptyList());
                    backupFileRef.set(file);
                } catch (Throwable t) {
                    uncaughtException.compareAndSet(null, t);
                } finally {
                    finishLatch.countDown();
                }
            });

            startLatch.countDown(); // Launch all operations simultaneously
            boolean finished = finishLatch.await(10, TimeUnit.SECONDS);

            assertTrue(finished, "Concurrent operations timed out");
            assertNull(uncaughtException.get(), "No exception should occur during concurrent backup and CRUD");

            File generatedBackup = backupFileRef.get();
            assertNotNull(generatedBackup, "Backup file should have been generated");
            assertTrue(generatedBackup.exists());

            // Verify backup archive is valid and can be read back cleanly
            BackupData restored = backupManager.restoreBackup(generatedBackup.getAbsolutePath());
            assertNotNull(restored);
            assertNotNull(restored.getTasks());
        } finally {
            crudPool.shutdownNow();
        }
    }

    @Test
    @DisplayName("BackupCompletedEvent delivered via EventBus executes on the EDT")
    void testBackupCompletedEventDeliveredOnEDT() throws Exception {
        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicBoolean deliveredOnEDT = new AtomicBoolean(false);

        EventBus.getInstance().subscribe(BackupCompletedEvent.class, event -> {
            deliveredOnEDT.set(SwingUtilities.isEventDispatchThread());
            eventLatch.countDown();
        });

        // Trigger backup from background thread pool
        ThreadPoolManager.getInstance().submit(() -> {
            assertFalse(SwingUtilities.isEventDispatchThread(), "Publisher must be background thread");
            try {
                File dummyFile = tempBackupDir.resolve("event_test.ser").toFile();
                dummyFile.createNewFile();
                EventBus.getInstance().publish(new BackupCompletedEvent(dummyFile, 5, 1, new java.util.Date()));
            } catch (Exception e) {
                // Ignore
            }
        });

        boolean delivered = eventLatch.await(3, TimeUnit.SECONDS);
        assertTrue(delivered, "BackupCompletedEvent delivery timed out");
        assertTrue(deliveredOnEDT.get(), "BackupCompletedEvent subscriber MUST execute on the EDT!");
    }
}
