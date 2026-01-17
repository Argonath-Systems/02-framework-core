package com.argonathsystems.framework.core.result;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents either a successful result or a failure with message.
 * Inspired by Rust's Result type.
 *
 * @param <T> The type of the success value
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    /**
     * Check if this is a success.
     *
     * @return true if success
     */
    boolean isSuccess();

    /**
     * Check if this is a failure.
     *
     * @return true if failure
     */
    boolean isFailure();

    /**
     * Get the value if success.
     *
     * @return Optional containing the value, or empty if failure
     */
    Optional<T> getValue();

    /**
     * Get the error message if failure.
     *
     * @return Optional containing the error, or empty if success
     */
    Optional<String> getError();

    /**
     * Map the success value to another type.
     *
     * @param mapper Function to transform the value
     * @param <U>    The new type
     * @return Mapped result
     */
    <U> Result<U> map(Function<T, U> mapper);

    /**
     * FlatMap the success value to another Result.
     *
     * @param mapper Function returning a new Result
     * @param <U>    The new type
     * @return The mapped result
     */
    <U> Result<U> flatMap(Function<T, Result<U>> mapper);

    /**
     * Get the value or a default.
     *
     * @param defaultValue Default to return on failure
     * @return The value or default
     */
    T orElse(T defaultValue);

    /**
     * Get the value or throw.
     *
     * @return The value
     * @throws ResultException if this is a failure
     */
    T orElseThrow();

    /**
     * Execute action if success.
     *
     * @param action Action to run with the value
     */
    void ifSuccess(Consumer<T> action);

    /**
     * Execute action if failure.
     *
     * @param action Action to run with the error message
     */
    void ifFailure(Consumer<String> action);

    // === Factory Methods ===

    /**
     * Create a success result.
     *
     * @param value The success value
     * @param <T>   The value type
     * @return A success result
     */
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Create a failure result.
     *
     * @param message The error message
     * @param <T>     The expected value type
     * @return A failure result
     */
    static <T> Result<T> failure(String message) {
        return new Failure<>(message);
    }

    /**
     * Create a failure result with formatted message.
     *
     * @param format Format string
     * @param args   Format arguments
     * @param <T>    The expected value type
     * @return A failure result
     */
    static <T> Result<T> failure(String format, Object... args) {
        return new Failure<>(String.format(format, args));
    }

    /**
     * Create a result from an Optional.
     *
     * @param optional     The optional value
     * @param errorMessage Error message if empty
     * @param <T>          The value type
     * @return Success if present, Failure if empty
     */
    static <T> Result<T> fromOptional(Optional<T> optional, String errorMessage) {
        return optional.map(Result::success)
                .orElseGet(() -> failure(errorMessage));
    }

    // === Implementations ===

    /**
     * Successful result containing a value.
     *
     * @param value The success value
     * @param <T>   The value type
     */
    record Success<T>(T value) implements Result<T> {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public Optional<T> getValue() {
            return Optional.ofNullable(value);
        }

        @Override
        public Optional<String> getError() {
            return Optional.empty();
        }

        @Override
        public <U> Result<U> map(Function<T, U> mapper) {
            return new Success<>(mapper.apply(value));
        }

        @Override
        public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public T orElse(T defaultValue) {
            return value;
        }

        @Override
        public T orElseThrow() {
            return value;
        }

        @Override
        public void ifSuccess(Consumer<T> action) {
            action.accept(value);
        }

        @Override
        public void ifFailure(Consumer<String> action) {
            // no-op
        }
    }

    /**
     * Failed result containing an error message.
     *
     * @param message The error message
     * @param <T>     The expected value type
     */
    record Failure<T>(String message) implements Result<T> {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public Optional<T> getValue() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getError() {
            return Optional.of(message);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U> map(Function<T, U> mapper) {
            return (Result<U>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
            return (Result<U>) this;
        }

        @Override
        public T orElse(T defaultValue) {
            return defaultValue;
        }

        @Override
        public T orElseThrow() {
            throw new ResultException(message);
        }

        @Override
        public void ifSuccess(Consumer<T> action) {
            // no-op
        }

        @Override
        public void ifFailure(Consumer<String> action) {
            action.accept(message);
        }
    }
}
