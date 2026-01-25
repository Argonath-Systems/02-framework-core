package com.argonathsystems.framework.core.ratelimit;

import java.time.Duration;

/**
 * Configuration for a rate limiter.
 *
 * @param cooldown    Minimum duration between actions (null for no cooldown)
 * @param burstLimit  Maximum actions within the burst window (0 for no limit)
 * @param burstWindow Duration of the burst window
 * @param hourlyLimit Maximum actions per hour (0 for no limit)
 * @param dailyLimit  Maximum actions per day (0 for no limit)
 */
public record RateLimitConfig(
        Duration cooldown,
        int burstLimit,
        Duration burstWindow,
        int hourlyLimit,
        int dailyLimit
) {

    /**
     * Creates a config with no limits.
     *
     * @return An unlimited configuration
     */
    public static RateLimitConfig unlimited() {
        return new RateLimitConfig(null, 0, Duration.ofMinutes(1), 0, 0);
    }
}
