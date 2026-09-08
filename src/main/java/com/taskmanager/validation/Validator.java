package com.taskmanager.validation;

/**
 * Generic contract for domain entity validators.
 *
 * @param <T> the type of entity being validated
 */
public interface Validator<T> {

    /**
     * Validates the provided entity.
     *
     * @param entity the entity to inspect
     * @return the resulting {@link ValidationResult}
     */
    ValidationResult validate(T entity);
}
