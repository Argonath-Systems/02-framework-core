package com.argonathsystems.framework.core.progress;

import java.time.Instant;

/**
 * Tracks progress toward a goal.
 * Immutable - operations return new instances.
 *
 * @param current     Current progress value
 * @param required    Required value to complete
 * @param completed   Whether the goal has been completed
 * @param completedAt Timestamp when the goal was completed (null if not completed)
 */
public record Progress(
        int current,
        int required,
        boolean completed,
        Instant completedAt
) {

    /**
     * Creates a Progress instance with validation.
     *
     * @throws IllegalArgumentException if required or current is negative
     */
    public Progress {
        if (required < 0) {
            throw new IllegalArgumentException("Required cannot be negative: " + required);
        }
        if (current < 0) {
            throw new IllegalArgumentException("Current cannot be negative: " + current);
        }
    }

    /**
     * Create new progress tracker starting at 0.
     *
     * @param required The required amount to complete
     * @return A new Progress instance at 0/required
     */
    public static Progress of(int required) {
        return new Progress(0, required, false, null);
    }

    /**
     * Create with initial progress.
     *
     * @param current  Initial progress value
     * @param required Required value to complete
     * @return A new Progress instance
     */
    public static Progress of(int current, int required) {
        int clampedCurrent = Math.min(Math.max(0, current), required);
        boolean completed = clampedCurrent >= required;
        return new Progress(
                clampedCurrent,
                required,
                completed,
                completed ? Instant.now() : null
        );
    }

    /**
     * Increment progress.
     *
     * @param amount Amount to increment by
     * @return Result containing new Progress and whether this caused completion
     */
    public ProgressResult increment(int amount) {
        if (completed) {
            return new ProgressResult(this, false);
        }

        int newCurrent = Math.min(current + amount, required);
        boolean nowComplete = newCurrent >= required;

        Progress newProgress = new Progress(
                newCurrent,
                required,
                nowComplete,
                nowComplete ? Instant.now() : null
        );

        return new ProgressResult(newProgress, nowComplete);
    }

    /**
     * Decrement progress.
     *
     * @param amount Amount to decrement by
     * @return Result containing new Progress (never triggers completion)
     */
    public ProgressResult decrement(int amount) {
        int newCurrent = Math.max(0, current - amount);
        boolean stillComplete = newCurrent >= required;

        Progress newProgress = new Progress(
                newCurrent,
                required,
                stillComplete,
                stillComplete ? completedAt : null
        );

        return new ProgressResult(newProgress, false);
    }

    /**
     * Set absolute progress.
     *
     * @param value New progress value
     * @return New Progress with the specified value
     */
    public Progress set(int value) {
        int newCurrent = Math.max(0, Math.min(value, required));
        boolean nowComplete = newCurrent >= required;
        return new Progress(
                newCurrent,
                required,
                nowComplete,
                nowComplete && !completed ? Instant.now() : completedAt
        );
    }

    /**
     * Reset progress to 0.
     *
     * @return New Progress at 0/required
     */
    public Progress reset() {
        return new Progress(0, required, false, null);
    }

    /**
     * Get completion percentage (0.0 to 1.0).
     *
     * @return Progress as a float from 0.0 to 1.0
     */
    public float percentage() {
        if (required == 0) {
            return 1.0f;
        }
        return (float) current / required;
    }

    /**
     * Get completion percentage (0 to 100).
     *
     * @return Progress as an int from 0 to 100
     */
    public int percentageInt() {
        return Math.round(percentage() * 100);
    }

    /**
     * Get remaining count.
     *
     * @return Amount remaining to complete
     */
    public int remaining() {
        return Math.max(0, required - current);
    }

    /**
     * Format as string like "5/10" or "10/10 ✓".
     *
     * @return Formatted progress string
     */
    public String format() {
        return completed
                ? String.format("%d/%d ✓", current, required)
                : String.format("%d/%d", current, required);
    }

    /**
     * Format with percentage like "5/10 (50%)".
     *
     * @return Formatted progress string with percentage
     */
    public String formatWithPercentage() {
        return completed
                ? String.format("%d/%d ✓ (100%%)", current, required)
                : String.format("%d/%d (%d%%)", current, required, percentageInt());
    }
}
