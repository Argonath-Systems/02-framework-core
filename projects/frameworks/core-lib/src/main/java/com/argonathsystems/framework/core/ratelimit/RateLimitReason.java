package com.argonathsystems.framework.core.ratelimit;

/**
 * Reasons for rate limit results.
 */
public enum RateLimitReason {
    /**
     * Action is allowed.
     */
    ALLOWED,

    /**
     * Cooldown is still active from last action.
     */
    COOLDOWN_ACTIVE,

    /**
     * Burst limit exceeded within the burst window.
     */
    BURST_LIMITED,

    /**
     * Hourly limit exceeded.
     */
    HOURLY_LIMITED,

    /**
     * Daily limit exceeded.
     */
    DAILY_LIMITED
}
