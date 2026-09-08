package com.taskmanager.validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsulates the outcome of entity validation, tracking field-specific error messages.
 */
public class ValidationResult {

    private final Map<String, String> fieldErrors = new LinkedHashMap<>();

    /**
     * Records a validation error for a specific field.
     *
     * @param field the name of the invalid field
     * @param message description of the validation failure
     */
    public void addError(String field, String message) {
        fieldErrors.put(field, message);
    }

    /**
     * Indicates whether the entity satisfies all validation rules.
     *
     * @return true if no validation errors were recorded, false otherwise
     */
    public boolean isValid() {
        return fieldErrors.isEmpty();
    }

    /**
     * Returns an unmodifiable map of all field errors.
     *
     * @return map of field name to error message
     */
    public Map<String, String> getErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }

    /**
     * Retrieves the error message for a specific field, if any.
     *
     * @param field the field name
     * @return the error message, or null if none
     */
    public String getError(String field) {
        return fieldErrors.get(field);
    }

    /**
     * Produces a formatted summary of all validation errors.
     *
     * @return composite error message
     */
    public String getErrorMessage() {
        if (fieldErrors.isEmpty()) {
            return "";
        }
        return String.join("; ", fieldErrors.values());
    }
}
