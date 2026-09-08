package com.taskmanager.repository;

import com.taskmanager.model.BaseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GenericRepository Tests")
class GenericRepositoryTest {

    private static class TestEntity extends BaseEntity {
        private static final long serialVersionUID = 1L;
        private String name;


        public TestEntity(String name) {
            super();
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private GenericRepository<TestEntity> repository;

    @BeforeEach
    void setUp() {
        repository = new GenericRepository<>(TestEntity.class);
    }

    @Test
    @DisplayName("getEntityClass returns the configured class")
    void testGetEntityClass() {
        assertEquals(TestEntity.class, repository.getEntityClass());
    }

    @Test
    @DisplayName("Constructor throws NullPointerException when entityClass is null")
    void testConstructorNullClass() {
        assertThrows(NullPointerException.class, () -> new GenericRepository<>(null));
    }

    @Test
    @DisplayName("save new entity generates sequential IDs starting at 1 and updates updatedAt")
    void testSaveNewEntity() {
        TestEntity entity1 = new TestEntity("First");
        LocalDateTime beforeSave = LocalDateTime.now().minusSeconds(1);

        TestEntity saved1 = repository.save(entity1);
        assertEquals(1, saved1.getId());
        assertNotNull(saved1.getUpdatedAt());
        assertFalse(saved1.getUpdatedAt().isBefore(beforeSave));

        TestEntity entity2 = new TestEntity("Second");
        TestEntity saved2 = repository.save(entity2);
        assertEquals(2, saved2.getId());
    }

    @Test
    @DisplayName("save existing entity preserves custom/existing id")
    void testSaveWithPresetId() {
        TestEntity entity = new TestEntity("Preset");
        entity.setId(99);

        TestEntity saved = repository.save(entity);
        assertEquals(99, saved.getId());
        assertEquals(1, repository.count());
        assertTrue(repository.existsById(99));

        // Update name and save again
        saved.setName("Updated Preset");
        repository.save(saved);
        assertEquals(1, repository.count());
        assertEquals("Updated Preset", repository.findById(99).orElseThrow().getName());
    }

    @Test
    @DisplayName("save throws IllegalArgumentException when entity is null")
    void testSaveNull() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    @DisplayName("findById returns Optional containing entity or empty if not present")
    void testFindById() {
        TestEntity entity = repository.save(new TestEntity("Alpha"));

        Optional<TestEntity> found = repository.findById(entity.getId());
        assertTrue(found.isPresent());
        assertEquals("Alpha", found.get().getName());

        Optional<TestEntity> missing = repository.findById(999);
        assertFalse(missing.isPresent());
    }

    @Test
    @DisplayName("findAll returns all persisted entities or empty list")
    void testFindAll() {
        assertTrue(repository.findAll().isEmpty());

        repository.save(new TestEntity("One"));
        repository.save(new TestEntity("Two"));
        repository.save(new TestEntity("Three"));

        List<TestEntity> all = repository.findAll();
        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("deleteById removes entity and returns true, or false if not found")
    void testDeleteById() {
        TestEntity entity = repository.save(new TestEntity("ToDelete"));
        int id = entity.getId();

        assertTrue(repository.existsById(id));
        assertTrue(repository.deleteById(id));
        assertFalse(repository.existsById(id));
        assertFalse(repository.findById(id).isPresent());

        // Deleting non-existent id returns false
        assertFalse(repository.deleteById(id));
        assertFalse(repository.deleteById(9999));
    }

    @Test
    @DisplayName("count and existsById reflect current repository state")
    void testCountAndExistsById() {
        assertEquals(0, repository.count());
        assertFalse(repository.existsById(1));

        TestEntity saved = repository.save(new TestEntity("Item"));
        assertEquals(1, repository.count());
        assertTrue(repository.existsById(saved.getId()));

        repository.deleteById(saved.getId());
        assertEquals(0, repository.count());
        assertFalse(repository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("Concurrent saves across 20 threads assign distinct IDs and retain all entities")
    void testConcurrentSaves() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        Set<Integer> assignedIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TestEntity saved = repository.save(new TestEntity("Concurrent-" + index));
                    assignedIds.add(saved.getId());
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent tasks should complete within timeout");
        assertTrue(errors.isEmpty(), "No errors should occur during concurrent saves");
        assertEquals(threadCount, repository.count());
        assertEquals(threadCount, assignedIds.size(), "Each thread should get a unique ID");
    }
}
