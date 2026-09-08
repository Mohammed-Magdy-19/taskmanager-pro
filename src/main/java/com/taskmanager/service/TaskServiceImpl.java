package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.repository.TaskRepository;

import java.time.LocalDateTime;

/**
 * Service implementation for managing {@link Task} entities.
 * Extends {@link GenericService} using the Template Method pattern.
 *
 * <p>
 * <em>Note: Inline validation logic implemented here is temporary until Phase 4
 * introduces the dedicated {@code Validator<T>} and {@code TaskValidator}
 * components.</em>
 * </p>
 */
public class TaskServiceImpl extends GenericService<Task> {

    /** Maximum permitted length for a task title. */
    public static final int MAX_TITLE_LENGTH = 100;

    private final TaskRepository taskRepository;

    /**
     * Constructs a task service with the given SQLite task repository.
     *
     * @param repository the task repository managing persistence
     */
    public TaskServiceImpl(TaskRepository repository) {
        super(repository);
        this.taskRepository = repository;
    }

    /**
     * Constructs a task service with a generic task repository (supports test
     * doubles).
     *
     * @param repository the generic repository managing task persistence
     */
    public TaskServiceImpl(com.taskmanager.repository.GenericRepository<Task> repository) {
        super(repository);
        this.taskRepository = (repository instanceof TaskRepository) ? (TaskRepository) repository : null;
    }

    /**
     * Retrieves the typed TaskRepository.
     *
     * @return the TaskRepository, or null if backed by another repository
     *         implementation
     */
    public TaskRepository getTaskRepository() {
        return taskRepository;
    }

    @Override
    protected void validateBeforeCreate(Task task) {
        validateTask(task);
    }

    @Override
    protected void validateBeforeUpdate(Task task) {
        validateTask(task);
    }

    @Override
    protected void validateBeforeDelete(int id) {
        // No pre-delete constraints in Phase 2
    }

    @Override
    protected void afterCreate(Task task) {
        // Empty hook: reserved for Phase 5 reminder scheduling
    }

    @Override
    protected void afterUpdate(Task task) {
        // Empty hook: reserved for Phase 5 reminder rescheduling
    }

    @Override
    protected void afterDelete(int id) {
        // Empty hook: reserved for Phase 5 reminder cancellation
    }

    /**
     * Performs domain validation on the task entity.
     *
     * @param task the task to validate
     * @throws IllegalArgumentException if validation rules are violated
     */
    private void validateTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be blank");
        }
        if (task.getTitle().trim().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "Task title cannot exceed " + MAX_TITLE_LENGTH + " characters");
        }
        if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Task due date cannot be in the past");
        }
    }
}
