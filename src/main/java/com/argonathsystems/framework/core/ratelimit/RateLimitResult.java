package com.argonathsystems.framework.core.ratelimit;

import java.time.Duration;

/**
 * Result of a rate limit check.
 *
 * @param allowed    Whether the action is allowed
 * @param reason     The reason for the result
 * @param retryAfter Duration until the action will be allowed (zero if allowed)
 */
public record RateLimitResult(
        boolean allowed,
        RateLimitReason reason,
        Duration retryAfter
) {

    /**
     * Creates an allowed result.
     *
     * @return A result indicating the action is allowed
     */
    public static RateLimitResult grant() {
        return new RateLimitResult(true, RateLimitReason.ALLOWED, Duration.ZERO);
    }

    /**
     * Creates a denied result.
     *
     * @param reason     The reason for denial
     * @param retryAfter Duration until retry is allowed
     * @return A result indicating the action is denied
     */
    public static RateLimitResult denied(RateLimitReason reason, Duration retryAfter) {
        return new RateLimitResult(false, reason, retryAfter);
    }
}
