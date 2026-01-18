package com.argonathsystems.framework.core.ratelimit;

import java.time.Duration;

/**
 * Generic rate limiter for any resource.
 *
 * @param <K> Key type (e.g., UUID for per-player limits)
 */
public interface RateLimiter<K> {

    /**
     * Check if action is allowed.
     *
     * @param key The key to check limits for
     * @return The result indicating if action is allowed
     */
    RateLimitResult checkLimit(K key);

    /**
     * Record that an action was performed.
     *
     * @param key The key to record the action for
     */
    void recordAction(K key);

    /**
     * Check and record atomically.
     *
     * @param key The key to acquire
     * @return true if action was allowed and recorded
     */
    boolean tryAcquire(K key);

    /**
     * Reset all limits for a key.
     *
     * @param key The key to reset
     */
    void reset(K key);

    /**
     * Get remaining quota.
     *
     * @param key The key to get status for
     * @return Current rate limit status
     */
    RateLimitStatus getStatus(K key);
}
