package com.taskmanager.service;

import com.taskmanager.model.Task;

/**
 * Interface contract for presenting task notification reminders to the user.
 */
public interface NotificationService {

    /**
     * Displays a notification reminder for the given task.
     *
     * @param task the task whose reminder has triggered
     */
    void showReminder(Task task);
}
