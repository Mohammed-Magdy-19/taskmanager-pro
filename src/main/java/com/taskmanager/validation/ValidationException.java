package com.taskmanager.validation;

import java.util.Objects;

/**
 * Unchecked exception thrown when domain entity validation fails.
 * Extends {@link IllegalArgumentException} for compatibility with service layer expectations.
 */
public class ValidationException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final ValidationResult validationResult;

    /**
     * Constructs a ValidationException wrapping the given validation outcome.
     *
     * @param validationResult the validation result containing errors
     */
    public ValidationException(ValidationResult validationResult) {
        super(Objects.requireNonNull(validationResult, "validationResult must not be null").getErrorMessage());
        this.validationResult = validationResult;
    }

    /**
     * Retrieves the underlying validation result.
     *
     * @return the validation result
     */
    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
