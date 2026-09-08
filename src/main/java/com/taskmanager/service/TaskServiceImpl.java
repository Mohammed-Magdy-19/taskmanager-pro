package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.repository.TaskRepository;

import com.taskmanager.concurrency.ReminderScheduler;
import com.taskmanager.validation.TaskValidator;
import com.taskmanager.validation.ValidationException;
import com.taskmanager.validation.ValidationResult;

/**
 * Service implementation for managing {@link Task} entities.
 * Extends {@link GenericService} using the Template Method pattern and
 * delegates
 * entity validation to {@link TaskValidator}.
 */
public class TaskServiceImpl extends GenericService<Task> {

    /** Maximum permitted length for a task title. */
    public static final int MAX_TITLE_LENGTH = TaskValidator.MAX_TITLE_LENGTH;

    private final TaskRepository taskRepository;
    private final TaskValidator validator = new TaskValidator();

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
        ReminderScheduler.getInstance().scheduleReminder(task);
    }

    @Override
    protected void afterUpdate(Task task) {
        ReminderScheduler.getInstance().cancelReminder(task.getId());
        ReminderScheduler.getInstance().scheduleReminder(task);
    }

    @Override
    protected void afterDelete(int id) {
        ReminderScheduler.getInstance().cancelReminder(id);
    }

    /**
     * Performs domain validation on the task entity using the centralized
     * {@link TaskValidator}.
     *
     * @param task the task to validate
     * @throws ValidationException if validation rules are violated
     */
    private void validateTask(Task task) {
        ValidationResult result = validator.validate(task);
        if (!result.isValid()) {
            throw new ValidationException(result);
        }
    }
}
