package com.taskmanager.service;

import com.taskmanager.concurrency.ReminderScheduler;
import com.taskmanager.event.EventBus;
import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import com.taskmanager.repository.DatabaseConnection;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests connecting TaskService -> TaskRepository -> Real SQLite file.
 * Strictly uses no Mockito or test doubles at any layer to verify complete stack correctness.
 */
@DisplayName("TaskService End-to-End Real SQLite Integration Tests")
class TaskServiceEndToEndIntegrationTest {

    private Path tempDbFile;
    private DatabaseConnection dbConnection;
    private TaskRepository repository;
    private TaskServiceImpl service;

    @BeforeEach
    void setUp() throws IOException {
        tempDbFile = Files.createTempFile("task_service_e2e_", ".db");
        dbConnection = DatabaseConnection.resetInstance(tempDbFile.toAbsolutePath().toString());
        repository = new TaskRepository(dbConnection);
        service = new TaskServiceImpl(repository);
        EventBus.getInstance().clear();
        ReminderScheduler.getInstance().cancelAllReminders();
    }

    @AfterEach
    void tearDown() {
        EventBus.getInstance().clear();
        ReminderScheduler.getInstance().cancelAllReminders();
        if (dbConnection != null) {
            dbConnection.close();
        }
        try {
            Files.deleteIfExists(tempDbFile);
        } catch (IOException ignored) {
        }
    }

    @Test
    @DisplayName("End-to-End: Create valid task persists in real SQLite and is retrievable via Service")
    void testEndToEndCreateAndRetrieve() {
        LocalDateTime due = LocalDateTime.now().plusDays(3);
        Task task = new Task("E2E Ship Release", "End-to-end real SQLite verification",
                Priority.HIGH, Status.IN_PROGRESS, due, "Release", "e2e,sqlite");

        Task created = service.create(task);
        assertTrue(created.getId() > 0, "Generated ID must be assigned by SQLite autoincrement");
        assertEquals("E2E Ship Release", created.getTitle());

        // Verify retrieval via Service
        Optional<Task> retrievedViaService = service.findById(created.getId());
        assertTrue(retrievedViaService.isPresent(), "Task must be present when queried through service");
        Task serviceTask = retrievedViaService.get();
        assertEquals("E2E Ship Release", serviceTask.getTitle());
        assertEquals("End-to-end real SQLite verification", serviceTask.getDescription());
        assertEquals(Priority.HIGH, serviceTask.getPriority());
        assertEquals(Status.IN_PROGRESS, serviceTask.getStatus());
        assertEquals(due, serviceTask.getDueDate());
        assertEquals("Release", serviceTask.getCategory());
        assertEquals("e2e,sqlite", serviceTask.getTags());

        // Verify direct state in Repository
        assertEquals(1, repository.count(), "Repository count in SQLite must reflect exactly one record");
    }

    @Test
    @DisplayName("End-to-End: Update modified task writes updates to SQLite and preserves row count")
    void testEndToEndUpdateAndPersistence() {
        Task task = new Task("Original E2E Title", "Initial note", Priority.LOW, Status.PENDING, null, "General", null);
        Task created = service.create(task);
        int id = created.getId();

        created.setTitle("Updated E2E Title");
        created.setDescription("Updated note");
        created.setPriority(Priority.MEDIUM);
        created.setStatus(Status.COMPLETED);
        created.setDueDate(LocalDateTime.now().plusDays(7));

        Task updated = service.update(created);
        assertEquals(id, updated.getId());
        assertEquals("Updated E2E Title", updated.getTitle());

        // Verify direct SQLite row persistence
        Task fromDb = repository.findById(id).orElseThrow();
        assertEquals("Updated E2E Title", fromDb.getTitle());
        assertEquals("Updated note", fromDb.getDescription());
        assertEquals(Priority.MEDIUM, fromDb.getPriority());
        assertEquals(Status.COMPLETED, fromDb.getStatus());
        assertNotNull(fromDb.getDueDate());
        assertEquals(1, repository.count(), "Update must not alter overall row count");
    }

    @Test
    @DisplayName("End-to-End: Delete removes record from SQLite database completely")
    void testEndToEndDelete() {
        Task task = new Task("To Be Deleted", "Will be removed", Priority.LOW, Status.PENDING, null, null, null);
        Task created = service.create(task);
        int id = created.getId();
        assertEquals(1, repository.count());

        service.delete(id);

        assertFalse(service.findById(id).isPresent(), "Task must be absent after deletion via service");
        assertFalse(repository.findById(id).isPresent(), "Task must be absent after deletion via repository");
        assertEquals(0, repository.count(), "SQLite table must be empty");
    }

    @Test
    @DisplayName("End-to-End: Validation failure blocks write and leaves SQLite table empty")
    void testEndToEndValidationBlocksPersistence() {
        // Blank title
        Task blankTitle = new Task("", "No title", Priority.HIGH, Status.PENDING, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.create(blankTitle));
        assertEquals(0, repository.count(), "Database must remain untouched after validation failure");

        // Past due date
        Task pastDue = new Task("Past Task", "Old", Priority.HIGH, Status.PENDING, LocalDateTime.now().minusDays(2), null, null);
        assertThrows(IllegalArgumentException.class, () -> service.create(pastDue));
        assertEquals(0, repository.count(), "Database must remain untouched after past due date failure");

        // Title exceeding limit
        Task longTitle = new Task("X".repeat(101), "Too long", Priority.HIGH, Status.PENDING, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.create(longTitle));
        assertEquals(0, repository.count(), "Database must remain untouched after title limit failure");
    }

    @Test
    @DisplayName("End-to-End: Update or Delete on non-existent ID throws EntityNotFoundException")
    void testEndToEndNonExistentEntityHandling() {
        Task nonExistent = new Task("Non existent", null, Priority.MEDIUM, Status.PENDING, null, null, null);
        nonExistent.setId(9999);

        assertThrows(EntityNotFoundException.class, () -> service.update(nonExistent));
        assertThrows(EntityNotFoundException.class, () -> service.delete(9999));
        assertEquals(0, repository.count());
    }
}
