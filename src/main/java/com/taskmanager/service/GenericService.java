package com.taskmanager.service;

import com.taskmanager.model.BaseEntity;
import com.taskmanager.repository.GenericRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Abstract service providing Template Method workflows for CRUD operations.
 * Subclasses implement domain-specific validation and event hooks.
 *
 * @param <T> the type of entity managed, extending {@link BaseEntity}
 */
public abstract class GenericService<T extends BaseEntity> {

    protected final GenericRepository<T> repository;

    /**
     * Constructs the generic service with the specified repository.
     *
     * @param repository the repository backing this service
     */
    public GenericService(GenericRepository<T> repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /**
     * Retrieves the backing repository.
     *
     * @return the generic repository
     */
    public GenericRepository<T> getRepository() {
        return repository;
    }

    /**
     * Creates and persists a new entity after executing validation and post-creation hooks.
     *
     * @param entity the entity to create
     * @return the created and persisted entity
     * @throws IllegalArgumentException if entity is null or validation fails
     */
    public T create(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        validateBeforeCreate(entity);
        T saved = repository.save(entity);
        afterCreate(saved);
        return saved;
    }

    /**
     * Updates an existing entity after executing validation and post-update hooks.
     *
     * @param entity the entity to update
     * @return the updated entity
     * @throws IllegalArgumentException if entity is null or validation fails
     * @throws EntityNotFoundException if the entity does not exist in the repository
     */
    public T update(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (entity.getId() == 0 || !repository.existsById(entity.getId())) {
            throw new EntityNotFoundException(repository.getEntityClass(), entity.getId());
        }
        validateBeforeUpdate(entity);
        T saved = repository.save(entity);
        afterUpdate(saved);
        return saved;
    }

    /**
     * Deletes an entity by its identifier after executing pre-delete validation and post-delete hooks.
     *
     * @param id the entity identifier
     * @throws EntityNotFoundException if the entity does not exist in the repository
     */
    public void delete(int id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException(repository.getEntityClass(), id);
        }
        validateBeforeDelete(id);
        repository.deleteById(id);
        afterDelete(id);
    }

    /**
     * Finds an entity by its identifier.
     *
     * @param id the entity identifier
     * @return an {@link Optional} containing the entity if found, or empty otherwise
     */
    public Optional<T> findById(int id) {
        return repository.findById(id);
    }

    /**
     * Retrieves all entities managed by this service.
     *
     * @return a list containing all entities
     */
    public List<T> findAll() {
        return repository.findAll();
    }

    /**
     * Hook called before persisting a newly created entity.
     *
     * @param entity the entity to validate
     */
    protected abstract void validateBeforeCreate(T entity);

    /**
     * Hook called before updating an existing entity.
     *
     * @param entity the entity to validate
     */
    protected abstract void validateBeforeUpdate(T entity);

    /**
     * Hook called before deleting an entity by id.
     *
     * @param id the entity identifier
     */
    protected abstract void validateBeforeDelete(int id);

    /**
     * Hook called immediately after a new entity has been persisted.
     *
     * @param entity the created entity
     */
    protected abstract void afterCreate(T entity);

    /**
     * Hook called immediately after an entity has been updated.
     *
     * @param entity the updated entity
     */
    protected abstract void afterUpdate(T entity);

    /**
     * Hook called immediately after an entity has been deleted.
     *
     * @param id the identifier of the deleted entity
     */
    protected abstract void afterDelete(int id);
}
