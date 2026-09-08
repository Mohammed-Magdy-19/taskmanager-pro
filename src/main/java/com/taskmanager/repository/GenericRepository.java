package com.taskmanager.repository;

import com.taskmanager.model.BaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generic in-memory repository implementing thread-safe CRUD operations.
 *
 * @param <T> the type of entity managed by this repository, extending {@link BaseEntity}
 */
public class GenericRepository<T extends BaseEntity> {

    private static final int INITIAL_ID = 1;

    protected final Class<T> entityClass;
    protected final Map<Integer, T> entities = new ConcurrentHashMap<>();
    protected final AtomicInteger idGenerator = new AtomicInteger(INITIAL_ID);

    /**
     * Constructs a generic repository for the specified entity class.
     *
     * @param entityClass the class of the entity
     */
    public GenericRepository(Class<T> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
    }

    /**
     * Retrieves the entity class managed by this repository.
     *
     * @return the entity class
     */
    public Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * Persists or updates an entity. If {@code entity.getId() == 0}, a new identifier
     * is generated and assigned. {@code updatedAt} is set to the current timestamp.
     *
     * @param entity the entity to save
     * @return the saved entity
     * @throws IllegalArgumentException if entity is null
     */
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (entity.getId() == 0) {
            entity.setId(idGenerator.getAndIncrement());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        entities.put(entity.getId(), entity);
        return entity;
    }

    /**
     * Finds an entity by its integer identifier.
     *
     * @param id the entity identifier
     * @return an {@link Optional} containing the entity if found, or empty otherwise
     */
    public Optional<T> findById(int id) {
        return Optional.ofNullable(entities.get(id));
    }

    /**
     * Retrieves all entities stored in this repository.
     *
     * @return a list containing all entities
     */
    public List<T> findAll() {
        return new ArrayList<>(entities.values());
    }

    /**
     * Deletes an entity by its identifier.
     *
     * @param id the entity identifier
     * @return true if an entity was removed, false otherwise
     */
    public boolean deleteById(int id) {
        return entities.remove(id) != null;
    }

    /**
     * Returns the total count of entities currently stored in the repository.
     *
     * @return the number of entities
     */
    public int count() {
        return entities.size();
    }

    /**
     * Checks whether an entity with the specified identifier exists.
     *
     * @param id the entity identifier
     * @return true if an entity exists with this id, false otherwise
     */
    public boolean existsById(int id) {
        return entities.containsKey(id);
    }
}
