package com.argonathsystems.framework.core.ratelimit;

import java.time.Duration;

/**
 * Current status of rate limits for a key.
 *
 * @param remainingBurst     Remaining actions in the current burst window
 * @param remainingHourly    Remaining actions in the current hour
 * @param remainingDaily     Remaining actions in the current day
 * @param cooldownRemaining  Time remaining in the current cooldown
 */
public record RateLimitStatus(
        int remainingBurst,
        int remainingHourly,
        int remainingDaily,
        Duration cooldownRemaining
) {

    /**
     * Creates a status indicating no limits.
     *
     * @return An unlimited status
     */
    public static RateLimitStatus unlimited() {
        return new RateLimitStatus(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Duration.ZERO
        );
    }
}
