package com.taskmanager.validation;

import com.taskmanager.model.Task;

/**
 * Validates domain rules for {@link Task} entities.
 * Serves as the single shared validation authority for both UI dialogs and the service layer.
 */
public class TaskValidator implements Validator<Task> {

    /** Field key for title errors. */
    public static final String FIELD_TITLE = "title";

    /** Field key for due date errors. */
    public static final String FIELD_DUE_DATE = "dueDate";

    /** Field key for general task errors. */
    public static final String FIELD_TASK = "task";

    /** Maximum permitted characters in a task title. */
    public static final int MAX_TITLE_LENGTH = 100;

    /** Error message for empty/blank titles. */
    public static final String MSG_TITLE_BLANK = "Task title cannot be blank";

    /** Error message for titles exceeding the length limit. */
    public static final String MSG_TITLE_TOO_LONG = "Task title cannot exceed " + MAX_TITLE_LENGTH + " characters";

    /** Error message for due dates in the past. */
    public static final String MSG_DUE_DATE_PAST = "Task due date cannot be in the past";

    /** Error message when a task instance is null. */
    public static final String MSG_TASK_NULL = "Task cannot be null";

    @Override
    public ValidationResult validate(Task task) {
        ValidationResult result = new ValidationResult();

        if (task == null) {
            result.addError(FIELD_TASK, MSG_TASK_NULL);
            return result;
        }

        if (!FieldValidators.notBlank(task.getTitle())) {
            result.addError(FIELD_TITLE, MSG_TITLE_BLANK);
        } else if (!FieldValidators.maxLength(task.getTitle(), MAX_TITLE_LENGTH)) {
            result.addError(FIELD_TITLE, MSG_TITLE_TOO_LONG);
        }

        if (!FieldValidators.isFutureOrNull(task.getDueDate())) {
            result.addError(FIELD_DUE_DATE, MSG_DUE_DATE_PAST);
        }

        return result;
    }
}
