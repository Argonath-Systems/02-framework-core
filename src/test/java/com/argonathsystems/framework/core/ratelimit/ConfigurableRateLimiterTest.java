package com.argonathsystems.framework.core.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ConfigurableRateLimiter")
class ConfigurableRateLimiterTest {

    @Nested
    @DisplayName("cooldown limit")
    class CooldownTests {

        private RateLimiter<String> limiter;

        @BeforeEach
        void setUp() {
            limiter = ConfigurableRateLimiter.<String>builder()
                    .cooldown(Duration.ofMillis(100))
                    .build();
        }

        @Test
        @DisplayName("allows first action")
        void allowsFirstAction() {
            RateLimitResult result = limiter.checkLimit("user1");
            assertThat(result.allowed()).isTrue();
        }

        @Test
        @DisplayName("denies action within cooldown")
        void deniesWithinCooldown() {
            limiter.recordAction("user1");
            RateLimitResult result = limiter.checkLimit("user1");
            
            assertThat(result.allowed()).isFalse();
            assertThat(result.reason()).isEqualTo(RateLimitReason.COOLDOWN_ACTIVE);
        }

        @Test
        @DisplayName("allows action after cooldown expires")
        void allowsAfterCooldown() throws InterruptedException {
            limiter.recordAction("user1");
            Thread.sleep(150); // Wait for cooldown
            
            RateLimitResult result = limiter.checkLimit("user1");
            assertThat(result.allowed()).isTrue();
        }

        @Test
        @DisplayName("tracks users independently")
        void tracksUsersIndependently() {
            limiter.recordAction("user1");
            
            RateLimitResult result = limiter.checkLimit("user2");
            assertThat(result.allowed()).isTrue();
        }
    }

    @Nested
    @DisplayName("burst limit")
    class BurstTests {

        private RateLimiter<String> limiter;

        @BeforeEach
        void setUp() {
            limiter = ConfigurableRateLimiter.<String>builder()
                    .burstLimit(3, Duration.ofSeconds(1))
                    .build();
        }

        @Test
        @DisplayName("allows actions up to burst limit")
        void allowsUpToLimit() {
            assertThat(limiter.tryAcquire("user1")).isTrue();
            assertThat(limiter.tryAcquire("user1")).isTrue();
            assertThat(limiter.tryAcquire("user1")).isTrue();
        }

        @Test
        @DisplayName("denies actions beyond burst limit")
        void deniesBeyondLimit() {
            limiter.tryAcquire("user1");
            limiter.tryAcquire("user1");
            limiter.tryAcquire("user1");
            
            RateLimitResult result = limiter.checkLimit("user1");
            assertThat(result.allowed()).isFalse();
            assertThat(result.reason()).isEqualTo(RateLimitReason.BURST_LIMITED);
        }
    }

    @Nested
    @DisplayName("tryAcquire")
    class TryAcquireTests {

        @Test
        @DisplayName("acquires and records atomically")
        void acquiresAtomically() {
            RateLimiter<String> limiter = ConfigurableRateLimiter.<String>builder()
                    .burstLimit(2)
                    .build();
            
            assertThat(limiter.tryAcquire("user1")).isTrue();
            assertThat(limiter.tryAcquire("user1")).isTrue();
            assertThat(limiter.tryAcquire("user1")).isFalse();
        }
    }

    @Nested
    @DisplayName("reset")
    class ResetTests {

        @Test
        @DisplayName("clears all limits for key")
        void clearsLimits() {
            RateLimiter<String> limiter = ConfigurableRateLimiter.<String>builder()
                    .burstLimit(1)
                    .build();
            
            limiter.tryAcquire("user1");
            assertThat(limiter.tryAcquire("user1")).isFalse();
            
            limiter.reset("user1");
            assertThat(limiter.tryAcquire("user1")).isTrue();
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatusTests {

        @Test
        @DisplayName("returns remaining counts")
        void returnsRemainingCounts() {
            RateLimiter<String> limiter = ConfigurableRateLimiter.<String>builder()
                    .burstLimit(5)
                    .hourlyLimit(100)
                    .dailyLimit(1000)
                    .build();
            
            limiter.tryAcquire("user1");
            limiter.tryAcquire("user1");
            
            RateLimitStatus status = limiter.getStatus("user1");
            assertThat(status.remainingBurst()).isEqualTo(3);
            assertThat(status.remainingHourly()).isEqualTo(98);
            assertThat(status.remainingDaily()).isEqualTo(998);
        }
    }
}
