package com.argonathsystems.framework.core.validation;

import com.argonathsystems.framework.core.result.Result;

import java.util.Collection;

/**
 * Common validation utilities.
 * Provides static methods for validating inputs.
 */
public final class Validators {

    private Validators() {
        // Utility class, no instantiation
    }

    // === Null Checks ===

    /**
     * Require a value to be non-null.
     *
     * @param value Value to check
     * @param name  Name for error message
     * @param <T>   Value type
     * @return The value if non-null
     * @throws IllegalArgumentException if null
     */
    public static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        return value;
    }

    /**
     * Validate that a value is non-null.
     *
     * @param value Value to check
     * @param name  Name for error message
     * @param <T>   Value type
     * @return Success with value, or Failure if null
     */
    public static <T> Result<T> validateNonNull(T value, String name) {
        if (value == null) {
            return Result.failure("%s cannot be null", name);
        }
        return Result.success(value);
    }

    // === String Validation ===

    /**
     * Require a string to be non-null and non-empty.
     *
     * @param value Value to check
     * @param name  Name for error message
     * @return The value if valid
     * @throws IllegalArgumentException if null or empty
     */
    public static String requireNonEmpty(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be null or empty");
        }
        return value;
    }

    /**
     * Require a string to be non-null and non-blank.
     *
     * @param value Value to check
     * @param name  Name for error message
     * @return The trimmed value if valid
     * @throws IllegalArgumentException if null or blank
     */
    public static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be null or blank");
        }
        return value.trim();
    }

    /**
     * Validate a string is non-empty.
     *
     * @param value Value to check
     * @param name  Name for error message
     * @return Success with value, or Failure if empty
     */
    public static Result<String> validateNonEmpty(String value, String name) {
        if (value == null || value.isEmpty()) {
            return Result.failure("%s cannot be null or empty", name);
        }
        return Result.success(value);
    }

    /**
     * Validate a string matches a pattern.
     *
     * @param value   Value to check
     * @param pattern Regex pattern
     * @param name    Name for error message
     * @return Success with value, or Failure if doesn't match
     */
    public static Result<String> validatePattern(String value, String pattern, String name) {
        if (value == null || !value.matches(pattern)) {
            return Result.failure("%s must match pattern: %s", name, pattern);
        }
        return Result.success(value);
    }

    // === Number Validation ===

    /**
     * Require a value to be positive.
     *
     * @param value Value to check
     * @param name  Name for error message
     * @return The value if positive
     * @throws IllegalArgumentException if not positive
     */
    public static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive, got: " + value);
        }
        return value;
    }

    /**
     * Require a value to be non-negative.
     *
     * @param value Value to check
     * @param name  Name for error message
     * @return The value if non-negative
     * @throws IllegalArgumentException if negative
     */
    public static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative, got: " + value);
        }
        return value;
    }

    /**
     * Require a value to be within a range (inclusive).
     *
     * @param value Value to check
     * @param min   Minimum value (inclusive)
     * @param max   Maximum value (inclusive)
     * @param name  Name for error message
     * @return The value if in range
     * @throws IllegalArgumentException if out of range
     */
    public static int requireInRange(int value, int min, int max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    name + " must be between " + min + " and " + max + ", got: " + value);
        }
        return value;
    }

    /**
     * Validate a value is positive.
     *
     * @param value Value to check
     * @param name  Name for error message
     * @return Success with value, or Failure if not positive
     */
    public static Result<Integer> validatePositive(int value, String name) {
        if (value <= 0) {
            return Result.failure("%s must be positive, got: %d", name, value);
        }
        return Result.success(value);
    }

    /**
     * Validate a value is in range.
     *
     * @param value Value to check
     * @param min   Minimum (inclusive)
     * @param max   Maximum (inclusive)
     * @param name  Name for error message
     * @return Success with value, or Failure if out of range
     */
    public static Result<Integer> validateInRange(int value, int min, int max, String name) {
        if (value < min || value > max) {
            return Result.failure("%s must be between %d and %d, got: %d", name, min, max, value);
        }
        return Result.success(value);
    }

    // === Double Validation ===

    /**
     * Require a double to be positive.
     *
     * @param value Value to check
     * @param name  Name for error message
     * @return The value if positive
     * @throws IllegalArgumentException if not positive
     */
    public static double requirePositive(double value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive, got: " + value);
        }
        return value;
    }

    /**
     * Require a double to be within a range.
     *
     * @param value Value to check
     * @param min   Minimum (inclusive)
     * @param max   Maximum (inclusive)
     * @param name  Name for error message
     * @return The value if in range
     * @throws IllegalArgumentException if out of range
     */
    public static double requireInRange(double value, double min, double max, String name) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    name + " must be between " + min + " and " + max + ", got: " + value);
        }
        return value;
    }

    // === Collection Validation ===

    /**
     * Require a collection to be non-null and non-empty.
     *
     * @param collection Collection to check
     * @param name       Name for error message
     * @param <T>        Element type
     * @param <C>        Collection type
     * @return The collection if valid
     * @throws IllegalArgumentException if null or empty
     */
    public static <T, C extends Collection<T>> C requireNonEmpty(C collection, String name) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be null or empty");
        }
        return collection;
    }

    /**
     * Validate a collection is non-empty.
     *
     * @param collection Collection to check
     * @param name       Name for error message
     * @param <T>        Element type
     * @param <C>        Collection type
     * @return Success with collection, or Failure if empty
     */
    public static <T, C extends Collection<T>> Result<C> validateNonEmpty(C collection, String name) {
        if (collection == null || collection.isEmpty()) {
            return Result.failure("%s cannot be null or empty", name);
        }
        return Result.success(collection);
    }
}
