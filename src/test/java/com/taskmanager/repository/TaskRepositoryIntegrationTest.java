package com.taskmanager.repository;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskRepository SQLite Integration Tests")
class TaskRepositoryIntegrationTest {

    private Path tempDbFile;
    private DatabaseConnection dbConnection;
    private TaskRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        tempDbFile = Files.createTempFile("task_test_", ".db");
        dbConnection = DatabaseConnection.resetInstance(tempDbFile.toAbsolutePath().toString());
        repository = new TaskRepository(dbConnection);
    }

    @AfterEach
    void tearDown() {
        if (dbConnection != null) {
            dbConnection.close();
        }
        try {
            Files.deleteIfExists(tempDbFile);
        } catch (IOException ignored) {
        }
    }

    @Test
    @DisplayName("Happy path CRUD operations persist and query correctly in SQLite")
    void testBasicCrud() {
        LocalDateTime due = LocalDateTime.now().plusDays(2);
        Task task = new Task("Deploy App", "Deploy to server", Priority.HIGH,
                Status.IN_PROGRESS, due, "DevOps", "deploy,prod");

        Task saved = repository.save(task);
        assertTrue(saved.getId() > 0);
        assertEquals(1, repository.count());

        Optional<Task> retrieved = repository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Deploy App", retrieved.get().getTitle());
        assertEquals("Deploy to server", retrieved.get().getDescription());
        assertEquals(Priority.HIGH, retrieved.get().getPriority());
        assertEquals(Status.IN_PROGRESS, retrieved.get().getStatus());
        assertEquals(due, retrieved.get().getDueDate());
        assertEquals("DevOps", retrieved.get().getCategory());
        assertEquals("deploy,prod", retrieved.get().getTags());

        List<Task> all = repository.findAll();
        assertEquals(1, all.size());

        assertTrue(repository.deleteById(saved.getId()));
        assertEquals(0, repository.count());
    }

    @Test
    @DisplayName("save with id=0 performs INSERT and returns generated id, save with id>0 performs UPDATE without duplicating")
    void testInsertAndUpdate() {
        Task task = new Task("Initial Title", "Initial Desc", Priority.LOW,
                Status.PENDING, null, "Work", "tag1");

        Task inserted = repository.save(task);
        int generatedId = inserted.getId();
        assertTrue(generatedId > 0);
        assertEquals(1, repository.count());

        // Update task with existing id
        inserted.setTitle("Modified Title");
        inserted.setStatus(Status.COMPLETED);
        Task updated = repository.save(inserted);

        assertEquals(generatedId, updated.getId());
        assertEquals(1, repository.count(), "UPDATE must not create a duplicate row");

        Task fromDb = repository.findById(generatedId).orElseThrow();
        assertEquals("Modified Title", fromDb.getTitle());
        assertEquals(Status.COMPLETED, fromDb.getStatus());
    }

    @Test
    @DisplayName("Concurrency: 20 simultaneous threads save tasks without SQLITE_BUSY errors and exact final count")
    void testConcurrentWrites() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Task task = new Task("Task " + index, "Desc " + index, Priority.MEDIUM,
                            Status.PENDING, LocalDateTime.now().plusDays(1), "Cat", "tag");
                    repository.save(task);
                } catch (Throwable t) {
                    errors.add(t.getCause() != null ? t.getCause() : t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent tasks must finish within timeout");
        assertTrue(errors.isEmpty(), "No errors or SQLITE_BUSY exceptions should occur: " + errors);
        assertEquals(threadCount, repository.count(), "All concurrent tasks must be successfully saved");
    }

    @Test
    @DisplayName("findByStatus and findByPriority return empty list when no matches exist")
    void testFilterNoMatches() {
        repository.save(new Task("Task A", "Desc", Priority.LOW, Status.PENDING, null, null, null));

        List<Task> highTasks = repository.findByPriority(Priority.HIGH);
        assertNotNull(highTasks);
        assertTrue(highTasks.isEmpty());

        List<Task> completedTasks = repository.findByStatus(Status.COMPLETED);
        assertNotNull(completedTasks);
        assertTrue(completedTasks.isEmpty());

        assertTrue(repository.findByPriority(null).isEmpty());
        assertTrue(repository.findByStatus(null).isEmpty());
    }

    @Test
    @DisplayName("searchByKeyword matches zero, one, and multiple rows without returning duplicate row when keyword in title and description")
    void testSearchByKeyword() {
        repository.save(new Task("Review PR", "Review pull request description",
                Priority.HIGH, Status.PENDING, null, null, null));
        repository.save(new Task("Write Documentation", "Document REST endpoints",
                Priority.MEDIUM, Status.PENDING, null, null, null));
        repository.save(new Task("PR Meeting", "Discuss PR guidelines and review",
                Priority.LOW, Status.COMPLETED, null, null, null));

        // Zero matches
        List<Task> noMatches = repository.searchByKeyword("NonExistentTerm");
        assertTrue(noMatches.isEmpty());

        // One match
        List<Task> docMatches = repository.searchByKeyword("Documentation");
        assertEquals(1, docMatches.size());
        assertEquals("Write Documentation", docMatches.get(0).getTitle());

        // Multiple matches
        List<Task> prMatches = repository.searchByKeyword("PR");
        assertEquals(2, prMatches.size());

        // Single row match when keyword is in BOTH title and description (must return row exactly once)
        List<Task> reviewMatches = repository.searchByKeyword("Review");
        assertEquals(2, reviewMatches.size());
        assertEquals(1, reviewMatches.stream().filter(t -> t.getTitle().equals("Review PR")).count());
    }

    @Test
    @DisplayName("searchByKeyword safely handles SQL special characters without error or injection")
    void testSearchSpecialCharacters() {
        repository.save(new Task("100% Complete", "Discount _special_ deal 'quotes' and \"double\"",
                Priority.HIGH, Status.COMPLETED, null, null, null));

        // Literal search for '%'
        List<Task> percentMatches = repository.searchByKeyword("%");
        assertEquals(1, percentMatches.size());
        assertEquals("100% Complete", percentMatches.get(0).getTitle());

        // Literal search for '_'
        List<Task> underscoreMatches = repository.searchByKeyword("_special_");
        assertEquals(1, underscoreMatches.size());

        // Quotes handling
        List<Task> singleQuoteMatches = repository.searchByKeyword("'quotes'");
        assertEquals(1, singleQuoteMatches.size());

        List<Task> doubleQuoteMatches = repository.searchByKeyword("\"double\"");
        assertEquals(1, doubleQuoteMatches.size());

        // Non-matching special char safely returns empty list
        List<Task> noMatch = repository.searchByKeyword("99%");
        assertTrue(noMatch.isEmpty());
    }

    @Test
    @DisplayName("deleteById on non-existent id does not throw and does not affect count")
    void testDeleteNonExistentId() {
        repository.save(new Task("Task 1", "Desc", Priority.LOW, Status.PENDING, null, null, null));
        assertEquals(1, repository.count());

        assertFalse(repository.deleteById(999));
        assertEquals(1, repository.count());
    }

    @Test
    @DisplayName("findById immediately after deleteById returns Optional.empty")
    void testFindImmediatelyAfterDelete() {
        Task task = repository.save(new Task("Ephemeral", null, Priority.MEDIUM, Status.PENDING, null, null, null));
        int id = task.getId();

        assertTrue(repository.findById(id).isPresent());
        assertTrue(repository.deleteById(id));
        assertFalse(repository.findById(id).isPresent());
    }

    @Test
    @DisplayName("Nullable columns (due_date, category, tags, description) round-trip properly")
    void testNullableColumnsRoundTrip() {
        Task nullTask = new Task("Minimal Task", null, null, null, null, null, null);
        Task saved = repository.save(nullTask);

        Task retrieved = repository.findById(saved.getId()).orElseThrow();
        assertEquals("Minimal Task", retrieved.getTitle());
        assertNull(retrieved.getDescription());
        assertNull(retrieved.getDueDate());
        assertNull(retrieved.getCategory());
        assertNull(retrieved.getTags());
        assertEquals(Priority.MEDIUM, retrieved.getPriority());
        assertEquals(Status.PENDING, retrieved.getStatus());
    }

    @Test
    @DisplayName("Very long text fields (several KB) save and retrieve without truncation")
    void testLargePayload() {
        String longDescription = "Detailed description paragraph. ".repeat(400); // ~12 KB text
        Task largeTask = new Task("Large Task", longDescription, Priority.HIGH, Status.PENDING, null, null, null);

        Task saved = repository.save(largeTask);
        Task retrieved = repository.findById(saved.getId()).orElseThrow();

        assertEquals(longDescription, retrieved.getDescription());
        assertEquals(longDescription.length(), retrieved.getDescription().length());
    }

    @Test
    @DisplayName("saveAll batch persists multiple tasks within a single transaction")
    void testSaveAllBatch() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            tasks.add(new Task("Batch Task " + i, "Batch Desc " + i, Priority.LOW, Status.PENDING, null, "Batch", "tag"));
        }

        repository.saveAll(tasks);
        assertEquals(25, repository.count());

        List<Task> all = repository.findAll();
        assertEquals(25, all.size());
    }
}
