package com.taskmanager.service;

/**
 * Exception thrown when an entity cannot be found in the repository.
 */
public class EntityNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public EntityNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a generated message for an entity class and id.
     *
     * @param entityClass the class of the entity
     * @param id the entity identifier
     */
    public EntityNotFoundException(Class<?> entityClass, int id) {
        super(String.format("%s with id %d was not found",
                entityClass != null ? entityClass.getSimpleName() : "Entity", id));
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
