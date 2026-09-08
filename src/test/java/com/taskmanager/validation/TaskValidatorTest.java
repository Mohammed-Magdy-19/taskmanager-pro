package com.taskmanager.validation;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskValidator Unit Tests")
class TaskValidatorTest {

    private TaskValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TaskValidator();
    }

    @Test
    @DisplayName("Valid task passes all validation checks")
    void testValidTask() {
        Task task = new Task("Complete documentation", "Write docs", Priority.MEDIUM,
                Status.PENDING, LocalDateTime.now().plusDays(2), "Docs", "v1");

        ValidationResult result = validator.validate(task);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Null task instance fails validation")
    void testNullTask() {
        ValidationResult result = validator.validate(null);
        assertFalse(result.isValid());
        assertEquals(TaskValidator.MSG_TASK_NULL, result.getError(TaskValidator.FIELD_TASK));
    }

    @Test
    @DisplayName("Blank, null, or whitespace-only title fails validation")
    void testTitleBlankValidation() {
        Task nullTitle = new Task(null, "Desc", null, null, null, null, null);
        ValidationResult r1 = validator.validate(nullTitle);
        assertFalse(r1.isValid());
        assertEquals(TaskValidator.MSG_TITLE_BLANK, r1.getError(TaskValidator.FIELD_TITLE));

        Task emptyTitle = new Task("", "Desc", null, null, null, null, null);
        ValidationResult r2 = validator.validate(emptyTitle);
        assertFalse(r2.isValid());
        assertEquals(TaskValidator.MSG_TITLE_BLANK, r2.getError(TaskValidator.FIELD_TITLE));

        Task whitespaceTitle = new Task("   \t  \n  ", "Desc", null, null, null, null, null);
        ValidationResult r3 = validator.validate(whitespaceTitle);
        assertFalse(r3.isValid());
        assertEquals(TaskValidator.MSG_TITLE_BLANK, r3.getError(TaskValidator.FIELD_TITLE));
    }

    @Test
    @DisplayName("Boundary test: Title length at 100 characters succeeds, 101 characters fails")
    void testTitleLengthBoundary() {
        String title100 = "x".repeat(100);
        Task task100 = new Task(title100, null, null, null, null, null, null);
        ValidationResult r100 = validator.validate(task100);
        assertTrue(r100.isValid());

        String title101 = "x".repeat(101);
        Task task101 = new Task(title101, null, null, null, null, null, null);
        ValidationResult r101 = validator.validate(task101);
        assertFalse(r101.isValid());
        assertEquals(TaskValidator.MSG_TITLE_TOO_LONG, r101.getError(TaskValidator.FIELD_TITLE));
    }

    @Test
    @DisplayName("Past due date fails, whereas null and future dates succeed")
    void testDueDateValidation() {
        Task pastTask = new Task("Task", null, null, null, LocalDateTime.now().minusMinutes(5), null, null);
        ValidationResult rPast = validator.validate(pastTask);
        assertFalse(rPast.isValid());
        assertEquals(TaskValidator.MSG_DUE_DATE_PAST, rPast.getError(TaskValidator.FIELD_DUE_DATE));

        Task nullDateTask = new Task("Task", null, null, null, null, null, null);
        ValidationResult rNullDate = validator.validate(nullDateTask);
        assertTrue(rNullDate.isValid());

        Task futureTask = new Task("Task", null, null, null, LocalDateTime.now().plusDays(10), null, null);
        ValidationResult rFuture = validator.validate(futureTask);
        assertTrue(rFuture.isValid());
    }

    @Test
    @DisplayName("Multiple violations in different fields are simultaneously collected")
    void testMultipleErrors() {
        Task invalidTask = new Task("", null, null, null, LocalDateTime.now().minusDays(1), null, null);
        ValidationResult result = validator.validate(invalidTask);

        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertEquals(TaskValidator.MSG_TITLE_BLANK, result.getError(TaskValidator.FIELD_TITLE));
        assertEquals(TaskValidator.MSG_DUE_DATE_PAST, result.getError(TaskValidator.FIELD_DUE_DATE));
        assertTrue(result.getErrorMessage().contains(TaskValidator.MSG_TITLE_BLANK));
        assertTrue(result.getErrorMessage().contains(TaskValidator.MSG_DUE_DATE_PAST));
    }
}
