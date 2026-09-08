package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import com.taskmanager.repository.GenericRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskServiceImpl Tests")
class TaskServiceImplTest {

    private GenericRepository<Task> repository;
    private TaskServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new GenericRepository<>(Task.class);
        service = new TaskServiceImpl(repository);
    }

    @Test
    @DisplayName("create successfully persists valid task and assigns ID")
    void testCreateValidTask() {
        Task task = new Task("Submit Tax Returns", "Quarterly submission",
                Priority.HIGH, Status.PENDING, LocalDateTime.now().plusDays(5), "Finance", "tax,q3");

        Task created = service.create(task);
        assertNotNull(created);
        assertEquals(1, created.getId());
        assertEquals("Submit Tax Returns", created.getTitle());
        assertEquals(1, repository.count());
    }

    @Test
    @DisplayName("create allows task with null due date")
    void testCreateWithNullDueDate() {
        Task task = new Task();
        task.setTitle("Task without due date");
        task.setDueDate(null);

        Task created = service.create(task);
        assertNotNull(created);
        assertNull(created.getDueDate());
    }

    @Test
    @DisplayName("create allows duplicate titles with independent IDs")
    void testDuplicateTitlesAllowed() {
        Task task1 = new Task();
        task1.setTitle("Duplicate Title");

        Task task2 = new Task();
        task2.setTitle("Duplicate Title");

        Task created1 = service.create(task1);
        Task created2 = service.create(task2);

        assertEquals(1, created1.getId());
        assertEquals(2, created2.getId());
        assertEquals(created1.getTitle(), created2.getTitle());
        assertEquals(2, repository.count());
    }

    @Test
    @DisplayName("create rejects null, empty, or whitespace-only title")
    void testTitleBlankValidation() {
        Task nullTitleTask = new Task();
        nullTitleTask.setTitle(null);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> service.create(nullTitleTask));
        assertTrue(ex1.getMessage().contains("cannot be blank"));

        Task emptyTitleTask = new Task();
        emptyTitleTask.setTitle("");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> service.create(emptyTitleTask));
        assertTrue(ex2.getMessage().contains("cannot be blank"));

        Task whitespaceTitleTask = new Task();
        whitespaceTitleTask.setTitle("   \t  \n  ");
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> service.create(whitespaceTitleTask));
        assertTrue(ex3.getMessage().contains("cannot be blank"));
    }

    @Test
    @DisplayName("Title length boundary: 100 characters succeeds, 101 characters throws")
    void testTitleLengthBoundary() {
        String title100 = "A".repeat(100);
        Task task100 = new Task();
        task100.setTitle(title100);
        Task created100 = service.create(task100);
        assertEquals(100, created100.getTitle().length());

        String title101 = "A".repeat(101);
        Task task101 = new Task();
        task101.setTitle(title101);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(task101));
        assertTrue(ex.getMessage().contains("cannot exceed 100 characters"));
    }

    @Test
    @DisplayName("create rejects due dates in the past")
    void testPastDueDateRejected() {
        Task pastTask = new Task();
        pastTask.setTitle("Past due task");
        pastTask.setDueDate(LocalDateTime.now().minusMinutes(5));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(pastTask));
        assertTrue(ex.getMessage().contains("cannot be in the past"));
    }

    @Test
    @DisplayName("create accepts due date in the near and far future")
    void testFutureDueDateAccepted() {
        Task nearFutureTask = new Task();
        nearFutureTask.setTitle("Near future");
        nearFutureTask.setDueDate(LocalDateTime.now().plusSeconds(10));
        assertDoesNotThrow(() -> service.create(nearFutureTask));

        Task farFutureTask = new Task();
        farFutureTask.setTitle("Far future");
        farFutureTask.setDueDate(LocalDateTime.now().plusYears(5));
        assertDoesNotThrow(() -> service.create(farFutureTask));
    }

    @Test
    @DisplayName("update validates modified task and saves changes")
    void testUpdateTask() {
        Task task = new Task();
        task.setTitle("Original Title");
        Task created = service.create(task);

        created.setTitle("Updated Title");
        created.setStatus(Status.IN_PROGRESS);
        Task updated = service.update(created);

        assertEquals("Updated Title", updated.getTitle());
        assertEquals(Status.IN_PROGRESS, updated.getStatus());

        Optional<Task> retrieved = service.findById(created.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Updated Title", retrieved.get().getTitle());
    }

    @Test
    @DisplayName("update rejects invalid title or past due date")
    void testUpdateInvalidValidation() {
        Task task = service.create(new Task("Valid", "Desc", null, null, null, null, null));

        task.setTitle("   ");
        assertThrows(IllegalArgumentException.class, () -> service.update(task));

        task.setTitle("Valid Again");
        task.setDueDate(LocalDateTime.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.update(task));
    }

    @Test
    @DisplayName("update and delete on non-existent id throw EntityNotFoundException")
    void testEntityNotFoundScenarios() {
        Task nonExistent = new Task();
        nonExistent.setId(999);
        nonExistent.setTitle("Ghost");

        assertThrows(EntityNotFoundException.class, () -> service.update(nonExistent));
        assertThrows(EntityNotFoundException.class, () -> service.delete(999));
    }

    @Test
    @DisplayName("delete removes existing task successfully")
    void testDeleteTask() {
        Task created = service.create(new Task("Delete Me", null, null, null, null, null, null));
        int id = created.getId();

        service.delete(id);

        assertFalse(service.findById(id).isPresent());
        assertEquals(0, repository.count());
    }
}
