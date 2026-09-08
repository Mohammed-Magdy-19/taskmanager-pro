package com.taskmanager.concurrency;

import com.taskmanager.event.EventBus;
import com.taskmanager.event.TaskReminderEvent;
import com.taskmanager.model.Task;
import com.taskmanager.util.AppLogger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Singleton scheduler managing task reminder triggers.
 * Maintains an in-memory registry of active reminder futures backed by {@link ConcurrentHashMap}.
 * Strictly adheres to the cancel-before-reschedule pattern to prevent duplicate reminder triggers.
 */
public final class ReminderScheduler {

    private static final AppLogger LOGGER = AppLogger.getLogger(ReminderScheduler.class);

    private static final ReminderScheduler INSTANCE = new ReminderScheduler();

    private final Map<Integer, ScheduledFuture<?>> reminders = new ConcurrentHashMap<>();

    private ReminderScheduler() {
        // Singleton
    }

    /**
     * Retrieves the singleton ReminderScheduler instance.
     *
     * @return the singleton ReminderScheduler
     */
    public static ReminderScheduler getInstance() {
        return INSTANCE;
    }

    /**
     * Schedules a reminder for the given task if its due date is in the future.
     * Cancels any previously active reminder for the task id before scheduling.
     * If the task's due date is null or already past/now, scheduling is safely skipped.
     *
     * @param task the task for which to schedule a reminder
     */
    public void scheduleReminder(Task task) {
        if (task == null || task.getDueDate() == null) {
            return;
        }

        // Cancel existing reminder if present before rescheduling (Rule 6)
        cancelReminder(task.getId());

        long delayMillis = Duration.between(LocalDateTime.now(), task.getDueDate()).toMillis();
        if (delayMillis <= 0) {
            LOGGER.debug("Skipping reminder for task " + task.getId() + "; due date is not in the future.");
            return;
        }

        int taskId = task.getId();
        ScheduledFuture<?> future = ThreadPoolManager.getInstance().schedule(() -> {
            try {
                LOGGER.info("Reminder triggered for task " + taskId + ": " + task.getTitle());
                EventBus.getInstance().publish(new TaskReminderEvent(task));
            } finally {
                reminders.remove(taskId);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);

        reminders.put(taskId, future);
        LOGGER.debug("Scheduled reminder for task " + taskId + " in " + delayMillis + " ms.");
    }

    /**
     * Cancels any active scheduled reminder for the specified task identifier.
     * Safe to call even if no reminder exists for the task.
     *
     * @param taskId the id of the task
     */
    public void cancelReminder(int taskId) {
        ScheduledFuture<?> future = reminders.remove(taskId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            LOGGER.debug("Cancelled active reminder for task id: " + taskId);
        }
    }

    /**
     * Checks if an active reminder future exists for the specified task id.
     *
     * @param taskId the id of the task
     * @return true if a reminder is currently scheduled and not yet done
     */
    public boolean hasActiveReminder(int taskId) {
        ScheduledFuture<?> future = reminders.get(taskId);
        return future != null && !future.isDone();
    }

    /**
     * Retrieves the count of currently scheduled active reminders.
     *
     * @return count of scheduled reminders
     */
    public int getActiveReminderCount() {
        return reminders.size();
    }

    /**
     * Cancels all currently scheduled reminders and clears the internal registry.
     */
    public void cancelAllReminders() {
        for (ScheduledFuture<?> future : reminders.values()) {
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
        }
        reminders.clear();
    }
}
