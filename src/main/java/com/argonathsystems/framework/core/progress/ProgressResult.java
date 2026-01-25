package com.argonathsystems.framework.core.progress;

/**
 * Result of a progress increment operation.
 *
 * @param progress      The updated Progress instance
 * @param justCompleted Whether this operation caused the progress to complete
 */
public record ProgressResult(
        Progress progress,
        boolean justCompleted
) {
}
