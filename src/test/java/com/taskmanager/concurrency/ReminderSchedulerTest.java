package com.taskmanager.concurrency;

import com.taskmanager.event.EventBus;
import com.taskmanager.event.TaskReminderEvent;
import com.taskmanager.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReminderSchedulerTest {

    private ReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = ReminderScheduler.getInstance();
        scheduler.cancelAllReminders();
        EventBus.getInstance().clear();
    }

    @AfterEach
    void tearDown() {
        scheduler.cancelAllReminders();
        EventBus.getInstance().clear();
    }

    @Test
    @DisplayName("scheduleReminder with due date in past or now does NOT schedule anything")
    void testPastDueDateDoesNotSchedule() {
        Task pastTask = new Task();
        pastTask.setId(1);
        pastTask.setTitle("Past Task");
        pastTask.setDueDate(LocalDateTime.now().minusMinutes(30));

        scheduler.scheduleReminder(pastTask);

        assertFalse(scheduler.hasActiveReminder(1), "Past task should not have an active reminder");
        assertEquals(0, scheduler.getActiveReminderCount(), "Active reminder count should be zero");
    }

    @Test
    @DisplayName("cancelReminder on a task with no active reminder does not throw")
    void testCancelReminderNonExistentDoesNotThrow() {
        assertDoesNotThrow(() -> {
            scheduler.cancelReminder(999);
            scheduler.cancelReminder(-1);
        });
    }

    @Test
    @DisplayName("Rapid schedule -> cancel -> schedule for same task ID results in exactly ONE event firing")
    void testRapidRescheduleFiresExactlyOnce() throws Exception {
        int taskId = 42;
        CountDownLatch fireLatch = new CountDownLatch(1);
        AtomicInteger triggerCount = new AtomicInteger(0);

        EventBus.getInstance().subscribe(TaskReminderEvent.class, event -> {
            if (event.getTask().getId() == taskId) {
                triggerCount.incrementAndGet();
                fireLatch.countDown();
            }
        });

        Task firstDraft = new Task();
        firstDraft.setId(taskId);
        firstDraft.setTitle("Draft 1");
        firstDraft.setDueDate(LocalDateTime.now().plusNanos(250_000_000)); // 250ms future

        Task secondDraft = new Task();
        secondDraft.setId(taskId);
        secondDraft.setTitle("Draft 2");
        secondDraft.setDueDate(LocalDateTime.now().plusNanos(280_000_000)); // 280ms future

        // Rapid schedule -> cancel -> schedule
        scheduler.scheduleReminder(firstDraft);
        scheduler.cancelReminder(taskId);
        scheduler.scheduleReminder(secondDraft);

        assertEquals(1, scheduler.getActiveReminderCount(), "Should have exactly one active reminder in map");
        assertTrue(scheduler.hasActiveReminder(taskId), "Task should have active reminder");

        boolean fired = fireLatch.await(3, TimeUnit.SECONDS);
        assertTrue(fired, "Reminder should have fired within timeout");

        // Give a short grace period to verify no second duplicate reminder fires
        Thread.sleep(200);
        assertEquals(1, triggerCount.get(), "Reminder must fire exactly once, never duplicate or stack");
    }

    @Test
    @DisplayName("Scheduling reminders for 20 tasks concurrently results in exactly 20 active entries")
    void testConcurrentScheduling20Tasks() throws Exception {
        int taskCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(taskCount);
        ExecutorService testExecutor = Executors.newFixedThreadPool(8);

        try {
            for (int i = 1; i <= taskCount; i++) {
                final int id = i;
                testExecutor.submit(() -> {
                    try {
                        startLatch.await(); // ensure all threads launch simultaneously
                        Task task = new Task();
                        task.setId(id);
                        task.setTitle("Task #" + id);
                        task.setDueDate(LocalDateTime.now().plusHours(1)); // future
                        scheduler.scheduleReminder(task);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // release all threads at once
            boolean finished = finishLatch.await(5, TimeUnit.SECONDS);
            assertTrue(finished, "All threads should finish scheduling within timeout");
            assertEquals(taskCount, scheduler.getActiveReminderCount(), "All 20 reminders must be registered");

            for (int i = 1; i <= taskCount; i++) {
                assertTrue(scheduler.hasActiveReminder(i), "Reminder for task " + i + " must exist");
            }
        } finally {
            testExecutor.shutdownNow();
        }
    }
}
