package com.taskmanager.validation;

import java.time.LocalDateTime;

/**
 * Reusable static validation predicates for domain field constraints.
 */
public final class FieldValidators {

    private FieldValidators() {
        // Static utility
    }

    /**
     * Checks if a string contains non-whitespace text.
     *
     * @param value the string to check
     * @return true if non-null and not blank, false otherwise
     */
    public static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Checks if a string length is within the maximum allowed boundary.
     *
     * @param value the string to check
     * @param max the maximum allowed character count
     * @return true if null or length <= max, false otherwise
     */
    public static boolean maxLength(String value, int max) {
        return value == null || value.trim().length() <= max;
    }

    /**
     * Checks if a timestamp is either null, present (current time), or in the future.
     *
     * @param dateTime the timestamp to evaluate
     * @return true if null or not strictly in the past, false otherwise
     */
    public static boolean isFutureOrNull(LocalDateTime dateTime) {
        return dateTime == null || !dateTime.isBefore(LocalDateTime.now());
    }
}
