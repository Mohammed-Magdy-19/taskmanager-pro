package com.taskmanager.event;

import com.taskmanager.model.Task;

import java.util.Objects;

/**
 * Immutable event published when a scheduled task reminder fires.
 * Carries an isolated snapshot of the task to notify UI subscribers and notification services.
 */
public final class TaskReminderEvent {

    private final Task task;

    /**
     * Constructs a TaskReminderEvent with an isolated snapshot of the task.
     *
     * @param task the reminder task
     */
    public TaskReminderEvent(Task task) {
        Objects.requireNonNull(task, "Task snapshot cannot be null");
        this.task = task.copy();
    }

    /**
     * Returns an independent snapshot of the reminder task.
     *
     * @return a snapshot of the task
     */
    public Task getTask() {
        return task.copy();
    }

    @Override
    public String toString() {
        return "TaskReminderEvent{taskId=" + task.getId() + ", title='" + task.getTitle() + "'}";
    }
}
