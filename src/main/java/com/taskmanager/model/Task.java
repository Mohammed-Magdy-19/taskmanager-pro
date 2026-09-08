package com.taskmanager.model;

import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;

import java.time.LocalDateTime;

/**
 * Domain entity representing a task in the Task Manager application.
 */
public class Task extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String title;
    private String description;
    private Priority priority = Priority.MEDIUM;
    private Status status = Status.PENDING;
    private LocalDateTime dueDate;
    private String category;
    private String tags;

    /**
     * Default constructor initializing task with default priority (MEDIUM) and status (PENDING).
     */
    public Task() {
        super();
    }

    /**
     * Parameterized constructor for convenience.
     *
     * @param title the task title
     * @param description the task description
     * @param priority the task priority
     * @param status the task status
     * @param dueDate the task due date
     * @param category the task category
     * @param tags comma-separated tags
     */
    public Task(String title, String description, Priority priority,
                Status status, LocalDateTime dueDate, String category, String tags) {
        super();
        this.title = title;
        this.description = description;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.status = status != null ? status : Status.PENDING;
        this.dueDate = dueDate;
        this.category = category;
        this.tags = tags;
    }

    /**
     * Gets the task title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the task title.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the task description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the task description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the task priority.
     *
     * @return the priority
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * Sets the task priority.
     *
     * @param priority the priority to set
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * Gets the task status.
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the task status.
     *
     * @param status the status to set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets the task due date.
     *
     * @return the due date
     */
    public LocalDateTime getDueDate() {
        return dueDate;
    }

    /**
     * Sets the task due date.
     *
     * @param dueDate the due date to set
     */
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Gets the task category.
     *
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the task category.
     *
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the comma-separated task tags.
     *
     * @return the tags
     */
    public String getTags() {
        return tags;
    }

    /**
     * Sets the comma-separated task tags.
     *
     * @param tags the tags to set
     */
    public void setTags(String tags) {
        this.tags = tags;
    }
}
