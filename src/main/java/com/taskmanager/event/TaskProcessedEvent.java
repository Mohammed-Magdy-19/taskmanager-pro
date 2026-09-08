package com.taskmanager.event;

import com.taskmanager.model.Task;

import java.util.Objects;

/**
 * Immutable event published whenever a task has been processed (created, updated, or deleted).
 * Holds an isolated snapshot of the affected task to prevent data races across threads.
 */
public final class TaskProcessedEvent {

    private final Task task;

    /**
     * Constructs a TaskProcessedEvent with an isolated snapshot of the task.
     *
     * @param task the processed task
     */
    public TaskProcessedEvent(Task task) {
        Objects.requireNonNull(task, "Task snapshot cannot be null");
        this.task = task.copy();
    }

    /**
     * Returns an independent snapshot of the processed task.
     *
     * @return a snapshot of the task
     */
    public Task getTask() {
        return task.copy();
    }

    @Override
    public String toString() {
        return "TaskProcessedEvent{taskId=" + task.getId() + ", title='" + task.getTitle() + "'}";
    }
}
