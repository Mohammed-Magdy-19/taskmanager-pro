package com.taskmanager.concurrency;

import com.taskmanager.config.ConfigManager;
import com.taskmanager.event.EventBus;
import com.taskmanager.event.TaskProcessedEvent;
import com.taskmanager.event.TaskReminderEvent;
import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.service.TaskServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrencyStressTest {

    private File tempDbFile;
    private TaskRepository repository;
    private TaskServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = Files.createTempFile("concurrency_stress_test_", ".db").toFile();
        ConfigManager.getInstance().setProperty(ConfigManager.KEY_DB_PATH, tempDbFile.getAbsolutePath());
        repository = new TaskRepository();
        service = new TaskServiceImpl(repository);
        EventBus.getInstance().clear();
        ReminderScheduler.getInstance().cancelAllReminders();
    }

    @AfterEach
    void tearDown() {
        EventBus.getInstance().clear();
        ReminderScheduler.getInstance().cancelAllReminders();
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    @DisplayName("Loop 3x: High-contention concurrent CRUD, search, and reminders with strict EDT and consistency assertions")
    void testConcurrentOperationsUnderContentionLoop3Times() throws Throwable {
        final int iterations = 3;
        for (int run = 1; run <= iterations; run++) {
            runStressIteration(run);
        }
    }

    private void runStressIteration(int iteration) throws Throwable {
        EventBus.getInstance().clear();
        ReminderScheduler.getInstance().cancelAllReminders();

        // Clear any tasks from previous iterations to isolate each run
        for (Task existing : repository.findAll()) {
            repository.deleteById(existing.getId());
        }

        final int createCount = 20;
        final int deleteCount = 8;
        final int searchOperations = 15;

        final AtomicReference<Throwable> uncaughtException = new AtomicReference<>();
        final AtomicBoolean offEdtObserved = new AtomicBoolean(false);
        final AtomicInteger processedEventsCount = new AtomicInteger(0);

        EventBus.getInstance().subscribe(TaskProcessedEvent.class, event -> {
            if (!SwingUtilities.isEventDispatchThread()) {
                offEdtObserved.set(true);
            }
            processedEventsCount.incrementAndGet();
        });

        EventBus.getInstance().subscribe(TaskReminderEvent.class, event -> {
            if (!SwingUtilities.isEventDispatchThread()) {
                offEdtObserved.set(true);
            }
        });

        // 1. Concurrently create tasks through ThreadPoolManager
        CountDownLatch createsLatch = new CountDownLatch(createCount);
        List<Task> createdTasks = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < createCount; i++) {
            final int index = i;
            ThreadPoolManager.getInstance().submit(() -> {
                try {
                    Task task = new Task();
                    task.setTitle("Stress Task " + iteration + "-" + index);
                    task.setDescription("Concurrent stress payload for item " + index);
                    task.setPriority(index % 2 == 0 ? Priority.HIGH : Priority.MEDIUM);
                    task.setStatus(Status.PENDING);
                    task.setDueDate(LocalDateTime.now().plusSeconds(index + 2)); // future due date for reminder

                    Task created = service.create(task);
                    createdTasks.add(created);
                    EventBus.getInstance().publish(new TaskProcessedEvent(created));
                } catch (Throwable t) {
                    uncaughtException.compareAndSet(null, t);
                } finally {
                    createsLatch.countDown();
                }
            });
        }

        boolean createsDone = createsLatch.await(10, TimeUnit.SECONDS);
        assertTrue(createsDone, "Iteration " + iteration + ": create tasks timed out");
        assertNull(uncaughtException.get(), "Iteration " + iteration + ": unexpected error during creates");
        assertEquals(createCount, createdTasks.size(), "Iteration " + iteration + ": all tasks should be created");

        // 2. Concurrently execute updates, deletes, and searches simultaneously
        ExecutorService stressPool = Executors.newFixedThreadPool(8);
        CountDownLatch concurrentLatch = new CountDownLatch(deleteCount + searchOperations + 5);

        try {
            // Delete tasks concurrently
            for (int i = 0; i < deleteCount; i++) {
                final Task toDelete = createdTasks.get(i);
                stressPool.submit(() -> {
                    try {
                        service.delete(toDelete.getId());
                        EventBus.getInstance().publish(new TaskProcessedEvent(toDelete));
                    } catch (Throwable t) {
                        uncaughtException.compareAndSet(null, t);
                    } finally {
                        concurrentLatch.countDown();
                    }
                });
            }

            // Update remaining tasks concurrently
            for (int i = deleteCount; i < deleteCount + 5; i++) {
                final Task toUpdate = createdTasks.get(i);
                stressPool.submit(() -> {
                    try {
                        toUpdate.setTitle(toUpdate.getTitle() + " [UPDATED]");
                        toUpdate.setStatus(Status.IN_PROGRESS);
                        Task updated = service.update(toUpdate);
                        EventBus.getInstance().publish(new TaskProcessedEvent(updated));
                    } catch (Throwable t) {
                        uncaughtException.compareAndSet(null, t);
                    } finally {
                        concurrentLatch.countDown();
                    }
                });
            }

            // Search queries concurrently
            for (int i = 0; i < searchOperations; i++) {
                stressPool.submit(() -> {
                    try {
                        List<Task> results = repository.searchByKeyword("Stress Task " + iteration);
                        assertFalse(results.isEmpty(), "Search should find existing stress tasks");
                    } catch (Throwable t) {
                        uncaughtException.compareAndSet(null, t);
                    } finally {
                        concurrentLatch.countDown();
                    }
                });
            }

            boolean operationsDone = concurrentLatch.await(10, TimeUnit.SECONDS);
            assertTrue(operationsDone, "Iteration " + iteration + ": concurrent operations timed out");
        } finally {
            stressPool.shutdownNow();
        }

        assertNull(uncaughtException.get(), "Iteration " + iteration + ": uncaught exception during concurrent execution");

        // Wait a brief interval for any pending EventBus invokeLater dispatches to complete
        CountDownLatch edtDrainLatch = new CountDownLatch(1);
        SwingUtilities.invokeLater(edtDrainLatch::countDown);
        edtDrainLatch.await(3, TimeUnit.SECONDS);

        // Verify Rule 1: No EventBus callback ran off EDT
        assertFalse(offEdtObserved.get(), "Iteration " + iteration + ": Observed EventBus delivery off the EDT!");

        // Verify Data Consistency: Initial - Deletes = Remaining
        int expectedRemaining = createCount - deleteCount;
        List<Task> remainingInDb = repository.findAll();
        assertEquals(expectedRemaining, remainingInDb.size(),
                "Iteration " + iteration + ": DB count must match createCount - deleteCount");
    }
}
