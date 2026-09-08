package com.taskmanager.service;

import com.taskmanager.model.BaseEntity;
import com.taskmanager.repository.GenericRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GenericService Template Method Tests")
class GenericServiceTest {

    private static class DummyEntity extends BaseEntity {
        private static final long serialVersionUID = 1L;
    }

    private static class TrackingRepository extends GenericRepository<DummyEntity> {
        final List<String> calls = new ArrayList<>();

        public TrackingRepository() {
            super(DummyEntity.class);
        }

        @Override
        public DummyEntity save(DummyEntity entity) {
            calls.add("repo.save");
            return super.save(entity);
        }

        @Override
        public Optional<DummyEntity> findById(int id) {
            calls.add("repo.findById");
            return super.findById(id);
        }

        @Override
        public List<DummyEntity> findAll() {
            calls.add("repo.findAll");
            return super.findAll();
        }

        @Override
        public boolean deleteById(int id) {
            calls.add("repo.deleteById");
            return super.deleteById(id);
        }

        @Override
        public boolean existsById(int id) {
            calls.add("repo.existsById");
            return super.existsById(id);
        }
    }

    private static class DummyService extends GenericService<DummyEntity> {
        private final List<String> invocations = new ArrayList<>();
        private boolean failValidation = false;

        public DummyService(GenericRepository<DummyEntity> repository) {
            super(repository);
        }

        public void setFailValidation(boolean failValidation) {
            this.failValidation = failValidation;
        }

        public List<String> getInvocations() {
            return invocations;
        }

        @Override
        protected void validateBeforeCreate(DummyEntity entity) {
            invocations.add("validateBeforeCreate");
            if (failValidation) {
                throw new IllegalArgumentException("Validation failed on create");
            }
        }

        @Override
        protected void validateBeforeUpdate(DummyEntity entity) {
            invocations.add("validateBeforeUpdate");
            if (failValidation) {
                throw new IllegalArgumentException("Validation failed on update");
            }
        }

        @Override
        protected void validateBeforeDelete(int id) {
            invocations.add("validateBeforeDelete");
            if (failValidation) {
                throw new IllegalArgumentException("Validation failed on delete");
            }
        }

        @Override
        protected void afterCreate(DummyEntity entity) {
            invocations.add("afterCreate");
        }

        @Override
        protected void afterUpdate(DummyEntity entity) {
            invocations.add("afterUpdate");
        }

        @Override
        protected void afterDelete(int id) {
            invocations.add("afterDelete");
        }
    }

    private TrackingRepository repository;
    private DummyService service;

    @BeforeEach
    void setUp() {
        repository = new TrackingRepository();
        service = new DummyService(repository);
    }

    @Test
    @DisplayName("Constructor throws NullPointerException when repository is null")
    void testConstructorNullRepository() {
        assertThrows(NullPointerException.class, () -> new DummyService(null));
    }

    @Test
    @DisplayName("getRepository returns configured repository")
    void testGetRepository() {
        assertSame(repository, service.getRepository());
    }

    @Test
    @DisplayName("create invokes validateBeforeCreate, saves to repository, and calls afterCreate in order")
    void testCreateWorkflow() {
        DummyEntity entity = new DummyEntity();
        DummyEntity saved = service.create(entity);

        assertNotNull(saved);
        assertEquals(1, saved.getId());
        assertEquals(List.of("validateBeforeCreate", "afterCreate"), service.getInvocations());
        assertTrue(repository.calls.contains("repo.save"));
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when entity is null")
    void testCreateNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> service.create(null));
        assertTrue(repository.calls.isEmpty());
    }

    @Test
    @DisplayName("create stops execution and does not save when validation fails")
    void testCreateValidationFailure() {
        service.setFailValidation(true);
        DummyEntity entity = new DummyEntity();

        assertThrows(IllegalArgumentException.class, () -> service.create(entity));
        assertEquals(List.of("validateBeforeCreate"), service.getInvocations());
        assertFalse(repository.calls.contains("repo.save"));
        assertEquals(0, repository.count());
    }

    @Test
    @DisplayName("update invokes validateBeforeUpdate, saves to repository, and calls afterUpdate in order")
    void testUpdateWorkflow() {
        DummyEntity entity = new DummyEntity();
        DummyEntity created = service.create(entity);
        service.getInvocations().clear();
        repository.calls.clear();

        DummyEntity updated = service.update(created);
        assertSame(created, updated);

        assertEquals(List.of("validateBeforeUpdate", "afterUpdate"), service.getInvocations());
        assertEquals(List.of("repo.existsById", "repo.save"), repository.calls);
    }

    @Test
    @DisplayName("update throws EntityNotFoundException when entity id does not exist")
    void testUpdateNonExistentEntity() {
        DummyEntity entity = new DummyEntity();
        entity.setId(999);

        assertThrows(EntityNotFoundException.class, () -> service.update(entity));
        assertTrue(service.getInvocations().isEmpty());
        assertEquals(List.of("repo.existsById"), repository.calls);
    }

    @Test
    @DisplayName("update throws IllegalArgumentException when entity is null")
    void testUpdateNullEntity() {
        assertThrows(IllegalArgumentException.class, () -> service.update(null));
    }

    @Test
    @DisplayName("update throws EntityNotFoundException when entity id is 0")
    void testUpdateZeroIdEntity() {
        DummyEntity unpersisted = new DummyEntity();
        unpersisted.setId(0);
        assertThrows(EntityNotFoundException.class, () -> service.update(unpersisted));
    }

    @Test
    @DisplayName("delete verifies existence, runs validateBeforeDelete, deletes from repo, and runs afterDelete")
    void testDeleteWorkflow() {
        DummyEntity entity = service.create(new DummyEntity());
        int id = entity.getId();
        service.getInvocations().clear();
        repository.calls.clear();

        service.delete(id);

        assertEquals(List.of("validateBeforeDelete", "afterDelete"), service.getInvocations());
        assertEquals(List.of("repo.existsById", "repo.deleteById"), repository.calls);
        assertFalse(repository.existsById(id));
    }

    @Test
    @DisplayName("delete throws EntityNotFoundException when id does not exist")
    void testDeleteNonExistentId() {
        assertThrows(EntityNotFoundException.class, () -> service.delete(88));
        assertTrue(service.getInvocations().isEmpty());
        assertEquals(List.of("repo.existsById"), repository.calls);
    }

    @Test
    @DisplayName("findById and findAll delegate directly to repository")
    void testFindDelegation() {
        DummyEntity entity = service.create(new DummyEntity());
        repository.calls.clear();

        Optional<DummyEntity> found = service.findById(entity.getId());
        assertTrue(found.isPresent());
        assertEquals(entity.getId(), found.get().getId());
        assertEquals(List.of("repo.findById"), repository.calls);

        repository.calls.clear();
        List<DummyEntity> all = service.findAll();
        assertEquals(1, all.size());
        assertEquals(List.of("repo.findAll"), repository.calls);
    }
}
