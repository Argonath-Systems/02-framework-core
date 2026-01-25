package com.argonathsystems.framework.core.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configurable rate limiter with multiple limit types.
 * Thread-safe implementation using ConcurrentHashMap.
 *
 * @param <K> Key type for per-key rate limiting
 */
public class ConfigurableRateLimiter<K> implements RateLimiter<K> {

    private final RateLimitConfig config;
    private final Map<K, RateLimitState> states;

    private ConfigurableRateLimiter(RateLimitConfig config) {
        this.config = config;
        this.states = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new builder for ConfigurableRateLimiter.
     *
     * @param <K> Key type
     * @return A new builder instance
     */
    public static <K> Builder<K> builder() {
        return new Builder<>();
    }

    @Override
    public RateLimitResult checkLimit(K key) {
        RateLimitState state = getOrCreateState(key);
        Instant now = Instant.now();

        synchronized (state) {
            // Check cooldown
            if (config.cooldown() != null && state.lastAction != null) {
                Duration sinceLastAction = Duration.between(state.lastAction, now);
                if (sinceLastAction.compareTo(config.cooldown()) < 0) {
                    return RateLimitResult.denied(
                            RateLimitReason.COOLDOWN_ACTIVE,
                            config.cooldown().minus(sinceLastAction)
                    );
                }
            }

            // Check burst limit
            if (config.burstLimit() > 0) {
                resetBurstIfNeeded(state, now);
                if (state.burstCount >= config.burstLimit()) {
                    return RateLimitResult.denied(
                            RateLimitReason.BURST_LIMITED,
                            Duration.between(now, state.burstWindowStart.plus(config.burstWindow()))
                    );
                }
            }

            // Check hourly limit
            if (config.hourlyLimit() > 0) {
                resetHourlyIfNeeded(state, now);
                if (state.hourlyCount >= config.hourlyLimit()) {
                    return RateLimitResult.denied(
                            RateLimitReason.HOURLY_LIMITED,
                            Duration.between(now, state.hourStart.plusSeconds(3600))
                    );
                }
            }

            // Check daily limit
            if (config.dailyLimit() > 0) {
                resetDailyIfNeeded(state, now);
                if (state.dailyCount >= config.dailyLimit()) {
                    return RateLimitResult.denied(
                            RateLimitReason.DAILY_LIMITED,
                            Duration.between(now, state.dayStart.plusSeconds(86400))
                    );
                }
            }
        }

        return RateLimitResult.grant();
    }

    @Override
    public void recordAction(K key) {
        RateLimitState state = getOrCreateState(key);
        Instant now = Instant.now();

        synchronized (state) {
            state.lastAction = now;

            if (config.burstLimit() > 0) {
                resetBurstIfNeeded(state, now);
                state.burstCount++;
            }

            if (config.hourlyLimit() > 0) {
                resetHourlyIfNeeded(state, now);
                state.hourlyCount++;
            }

            if (config.dailyLimit() > 0) {
                resetDailyIfNeeded(state, now);
                state.dailyCount++;
            }
        }
    }

    @Override
    public boolean tryAcquire(K key) {
        RateLimitState state = getOrCreateState(key);

        synchronized (state) {
            RateLimitResult result = checkLimit(key);
            if (result.allowed()) {
                recordAction(key);
                return true;
            }
        }
        return false;
    }

    @Override
    public void reset(K key) {
        states.remove(key);
    }

    @Override
    public RateLimitStatus getStatus(K key) {
        RateLimitState state = getOrCreateState(key);
        Instant now = Instant.now();

        synchronized (state) {
            resetBurstIfNeeded(state, now);
            resetHourlyIfNeeded(state, now);
            resetDailyIfNeeded(state, now);

            Duration cooldownRemaining = Duration.ZERO;
            if (config.cooldown() != null && state.lastAction != null) {
                Duration sinceLastAction = Duration.between(state.lastAction, now);
                if (sinceLastAction.compareTo(config.cooldown()) < 0) {
                    cooldownRemaining = config.cooldown().minus(sinceLastAction);
                }
            }

            return new RateLimitStatus(
                    config.burstLimit() > 0 ? Math.max(0, config.burstLimit() - state.burstCount) : Integer.MAX_VALUE,
                    config.hourlyLimit() > 0 ? Math.max(0, config.hourlyLimit() - state.hourlyCount) : Integer.MAX_VALUE,
                    config.dailyLimit() > 0 ? Math.max(0, config.dailyLimit() - state.dailyCount) : Integer.MAX_VALUE,
                    cooldownRemaining
            );
        }
    }

    private RateLimitState getOrCreateState(K key) {
        return states.computeIfAbsent(key, _ -> new RateLimitState());
    }

    private void resetBurstIfNeeded(RateLimitState state, Instant now) {
        if (state.burstWindowStart == null ||
                Duration.between(state.burstWindowStart, now).compareTo(config.burstWindow()) >= 0) {
            state.burstWindowStart = now;
            state.burstCount = 0;
        }
    }

    private void resetHourlyIfNeeded(RateLimitState state, Instant now) {
        if (state.hourStart == null ||
                Duration.between(state.hourStart, now).toSeconds() >= 3600) {
            state.hourStart = now;
            state.hourlyCount = 0;
        }
    }

    private void resetDailyIfNeeded(RateLimitState state, Instant now) {
        if (state.dayStart == null ||
                Duration.between(state.dayStart, now).toSeconds() >= 86400) {
            state.dayStart = now;
            state.dailyCount = 0;
        }
    }

    /**
     * Internal mutable state for tracking rate limits.
     */
    private static class RateLimitState {
        Instant lastAction;
        Instant burstWindowStart;
        int burstCount;
        Instant hourStart;
        int hourlyCount;
        Instant dayStart;
        int dailyCount;
    }

    /**
     * Builder for creating ConfigurableRateLimiter instances.
     *
     * @param <K> Key type
     */
    public static class Builder<K> {
        private Duration cooldown;
        private int burstLimit;
        private Duration burstWindow = Duration.ofMinutes(1);
        private int hourlyLimit;
        private int dailyLimit;

        /**
         * Set the cooldown between actions.
         *
         * @param cooldown Minimum duration between actions
         * @return This builder
         */
        public Builder<K> cooldown(Duration cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        /**
         * Set the burst limit.
         *
         * @param limit  Maximum actions within the window
         * @param window Duration of the burst window
         * @return This builder
         */
        public Builder<K> burstLimit(int limit, Duration window) {
            this.burstLimit = limit;
            this.burstWindow = window;
            return this;
        }

        /**
         * Set the burst limit with default 1-minute window.
         *
         * @param limit Maximum actions per minute
         * @return This builder
         */
        public Builder<K> burstLimit(int limit) {
            this.burstLimit = limit;
            return this;
        }

        /**
         * Set the hourly limit.
         *
         * @param limit Maximum actions per hour
         * @return This builder
         */
        public Builder<K> hourlyLimit(int limit) {
            this.hourlyLimit = limit;
            return this;
        }

        /**
         * Set the daily limit.
         *
         * @param limit Maximum actions per day
         * @return This builder
         */
        public Builder<K> dailyLimit(int limit) {
            this.dailyLimit = limit;
            return this;
        }

        /**
         * Build the rate limiter.
         *
         * @return A new ConfigurableRateLimiter instance
         */
        public RateLimiter<K> build() {
            return new ConfigurableRateLimiter<>(
                    new RateLimitConfig(cooldown, burstLimit, burstWindow, hourlyLimit, dailyLimit)
            );
        }
    }
}
