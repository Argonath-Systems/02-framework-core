# Core Library

## C4 Component Diagram

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

title Component Diagram for Core Library

Container(core_lib, "Core Library", "Java Library", "Common reusable utilities")

Container_Boundary(core_boundary, "Core Modules") {
    Component(random, "WeightedSelector", "Utils", "Weighted random selection")
    Component(ratelimit, "RateLimiter", "Utils", "Request rate limiting")
    Component(progress, "ProgressTracker", "Utils", "Generic progress tracking")
    Component(config, "ConfigSerializer", "Utils", "Configuration handling")
    Component(debug, "ExecutionTrace", "Utils", "Debug tracing")
    Component(result, "Result Types", "Utils", "Functional result types")
}

System_Ext(accessor, "Accessor API", "Dependency")
System_Ext(mod, "Standalone Mod", "Uses Core Lib")

Rel(core_lib, accessor, "Uses")
Rel(mod, core_lib, "Uses")
@enduml
```

## Specification

# SF-02: Core Library

> [!abstract] Overview
> A collection of common utilities used across all standalone mods and framework libraries. Provides reusable implementations for weighted random selection, rate limiting, progress tracking, configuration serialization, and debug tracing.

> [!tip] Zero Platform Dependency
> This library has **ZERO Hytale imports**. It depends only on SF-01 Accessor API for platform-agnostic data types.

## Product Identity

| Attribute | Value |
|-----------|-------|
| **Mod ID** | `sf-core-lib` |
| **Maven Artifact** | `com.lordofthetales.framework:core-lib` |
| **License** | MIT (free, open source) |
| **Target Audience** | All mod developers |
| **Dependencies** | SF-01 Accessor API (interfaces only) |

---

## Features Overview

| Feature | Package | Purpose |
|---------|---------|---------|
| WeightedSelector | `core.random` | Weighted random selection from pools |
| RateLimiter | `core.ratelimit` | Cooldowns, burst limits, hourly caps |
| ProgressTracker | `core.progress` | Track progress toward goals |
| ConfigSerializer | `core.config` | YAML/JSON config serialization |
| ExecutionTrace | `core.debug` | Debug tracing with timing |
| Result | `core.result` | Success/failure result types |
| Validators | `core.validation` | Input validation utilities |

---

## CL-001: Weighted Random Selection

### Interface

```java
package com.lordofthetales.framework.core.random;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Selects items from a weighted pool.
 * Thread-safe and reusable.
 */
public interface WeightedSelector<T> {
    
    /**
     * Select one item based on weights.
     * @param entries Weighted entries to select from
     * @param random Random generator to use
     * @return Selected item, or empty if pool is empty
     */
    Optional<T> selectOne(List<WeightedEntry<T>> entries, RandomGenerator random);
    
    /**
     * Select multiple items.
     * @param entries Pool to select from
     * @param count Number of items to select
     * @param allowDuplicates Whether same item can be selected multiple times
     * @param random Random generator
     * @return List of selected items
     */
    List<T> selectMultiple(
        List<WeightedEntry<T>> entries, 
        int count, 
        boolean allowDuplicates, 
        RandomGenerator random
    );
    
    /**
     * Select one item, filtering by predicate first.
     */
    Optional<T> selectOneWhere(
        List<WeightedEntry<T>> entries,
        java.util.function.Predicate<T> filter,
        RandomGenerator random
    );
}

/**
 * Entry in a weighted pool.
 */
public record WeightedEntry<T>(T item, double weight) {
    
    public WeightedEntry {
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }
    }
    
    public static <T> WeightedEntry<T> of(T item, double weight) {
        return new WeightedEntry<>(item, weight);
    }
    
    /**
     * Create entry with modified weight.
     */
    public WeightedEntry<T> withWeight(double newWeight) {
        return new WeightedEntry<>(item, newWeight);
    }
    
    /**
     * Scale weight by factor.
     */
    public WeightedEntry<T> scaleWeight(double factor) {
        return new WeightedEntry<>(item, weight * factor);
    }
}
```

### Implementation

```java
package com.lordofthetales.framework.core.random;

import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Default implementation of WeightedSelector.
 */
public class DefaultWeightedSelector<T> implements WeightedSelector<T> {
    
    private static final DefaultWeightedSelector<?> INSTANCE = new DefaultWeightedSelector<>();
    
    @SuppressWarnings("unchecked")
    public static <T> WeightedSelector<T> getInstance() {
        return (WeightedSelector<T>) INSTANCE;
    }
    
    @Override
    public Optional<T> selectOne(List<WeightedEntry<T>> entries, RandomGenerator random) {
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        
        double totalWeight = entries.stream()
            .mapToDouble(WeightedEntry::weight)
            .sum();
        
        if (totalWeight <= 0) {
            return Optional.empty();
        }
        
        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0;
        
        for (WeightedEntry<T> entry : entries) {
            cumulative += entry.weight();
            if (roll <= cumulative) {
                return Optional.of(entry.item());
            }
        }
        
        // Fallback (shouldn't happen with proper math)
        return Optional.of(entries.get(entries.size() - 1).item());
    }
    
    @Override
    public List<T> selectMultiple(
            List<WeightedEntry<T>> entries, 
            int count, 
            boolean allowDuplicates, 
            RandomGenerator random) {
        
        if (entries.isEmpty() || count <= 0) {
            return List.of();
        }
        
        List<T> results = new ArrayList<>(count);
        
        if (allowDuplicates) {
            for (int i = 0; i < count; i++) {
                selectOne(entries, random).ifPresent(results::add);
            }
        } else {
            List<WeightedEntry<T>> remaining = new ArrayList<>(entries);
            for (int i = 0; i < count && !remaining.isEmpty(); i++) {
                selectOne(remaining, random).ifPresent(selected -> {
                    results.add(selected);
                    remaining.removeIf(e -> e.item().equals(selected));
                });
            }
        }
        
        return results;
    }
    
    @Override
    public Optional<T> selectOneWhere(
            List<WeightedEntry<T>> entries,
            java.util.function.Predicate<T> filter,
            RandomGenerator random) {
        
        List<WeightedEntry<T>> filtered = entries.stream()
            .filter(e -> filter.test(e.item()))
            .toList();
        
        return selectOne(filtered, random);
    }
}
```

---

## CL-002: Rate Limiting

### Interface

```java
package com.lordofthetales.framework.core.ratelimit;

import java.time.Duration;
import java.time.Instant;

/**
 * Generic rate limiter for any resource.
 * @param <K> Key type (e.g., UUID for per-player limits)
 */
public interface RateLimiter<K> {
    
    /**
     * Check if action is allowed.
     */
    RateLimitResult checkLimit(K key);
    
    /**
     * Record that an action was performed.
     */
    void recordAction(K key);
    
    /**
     * Check and record atomically.
     * @return true if action was allowed and recorded
     */
    boolean tryAcquire(K key);
    
    /**
     * Reset all limits for a key.
     */
    void reset(K key);
    
    /**
     * Get remaining quota.
     */
    RateLimitStatus getStatus(K key);
}

/**
 * Result of a rate limit check.
 */
public record RateLimitResult(
    boolean allowed,
    RateLimitReason reason,
    Duration retryAfter
) {
    public static RateLimitResult allowed() {
        return new RateLimitResult(true, RateLimitReason.ALLOWED, Duration.ZERO);
    }
    
    public static RateLimitResult denied(RateLimitReason reason, Duration retryAfter) {
        return new RateLimitResult(false, reason, retryAfter);
    }
}

public enum RateLimitReason {
    ALLOWED,
    COOLDOWN_ACTIVE,
    BURST_LIMITED,
    HOURLY_LIMITED,
    DAILY_LIMITED
}

/**
 * Current status of rate limits.
 */
public record RateLimitStatus(
    int remainingBurst,
    int remainingHourly,
    int remainingDaily,
    Duration cooldownRemaining
) {}
```

### Builder & Implementation

```java
package com.lordofthetales.framework.core.ratelimit;

import java.time.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configurable rate limiter with multiple limit types.
 */
public class ConfigurableRateLimiter<K> implements RateLimiter<K> {
    
    private final RateLimitConfig config;
    private final Map<K, RateLimitState> states;
    
    private ConfigurableRateLimiter(RateLimitConfig config) {
        this.config = config;
        this.states = new ConcurrentHashMap<>();
    }
    
    public static <K> Builder<K> builder() {
        return new Builder<>();
    }
    
    @Override
    public RateLimitResult checkLimit(K key) {
        RateLimitState state = getOrCreateState(key);
        Instant now = Instant.now();
        
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
        
        return RateLimitResult.allowed();
    }
    
    @Override
    public void recordAction(K key) {
        RateLimitState state = getOrCreateState(key);
        Instant now = Instant.now();
        
        state.lastAction = now;
        state.burstCount++;
        state.hourlyCount++;
        state.dailyCount++;
    }
    
    @Override
    public boolean tryAcquire(K key) {
        RateLimitResult result = checkLimit(key);
        if (result.allowed()) {
            recordAction(key);
            return true;
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
            Math.max(0, config.burstLimit() - state.burstCount),
            Math.max(0, config.hourlyLimit() - state.hourlyCount),
            Math.max(0, config.dailyLimit() - state.dailyCount),
            cooldownRemaining
        );
    }
    
    // ... helper methods ...
    
    public static class Builder<K> {
        private Duration cooldown;
        private int burstLimit;
        private Duration burstWindow = Duration.ofMinutes(1);
        private int hourlyLimit;
        private int dailyLimit;
        
        public Builder<K> cooldown(Duration cooldown) {
            this.cooldown = cooldown;
            return this;
        }
        
        public Builder<K> burstLimit(int limit, Duration window) {
            this.burstLimit = limit;
            this.burstWindow = window;
            return this;
        }
        
        public Builder<K> hourlyLimit(int limit) {
            this.hourlyLimit = limit;
            return this;
        }
        
        public Builder<K> dailyLimit(int limit) {
            this.dailyLimit = limit;
            return this;
        }
        
        public RateLimiter<K> build() {
            return new ConfigurableRateLimiter<>(
                new RateLimitConfig(cooldown, burstLimit, burstWindow, hourlyLimit, dailyLimit)
            );
        }
    }
}

record RateLimitConfig(
    Duration cooldown,
    int burstLimit,
    Duration burstWindow,
    int hourlyLimit,
    int dailyLimit
) {}
```

---

## CL-003: Progress Tracking

```java
package com.lordofthetales.framework.core.progress;

import java.time.Instant;
import java.util.Optional;

/**
 * Tracks progress toward a goal.
 * Immutable - operations return new instances.
 */
public record Progress(
    int current,
    int required,
    boolean completed,
    Instant completedAt
) {
    
    public Progress {
        if (required < 0) throw new IllegalArgumentException("Required cannot be negative");
        if (current < 0) throw new IllegalArgumentException("Current cannot be negative");
    }
    
    /**
     * Create new progress tracker.
     */
    public static Progress of(int required) {
        return new Progress(0, required, false, null);
    }
    
    /**
     * Create with initial progress.
     */
    public static Progress of(int current, int required) {
        boolean completed = current >= required;
        return new Progress(
            Math.min(current, required),
            required,
            completed,
            completed ? Instant.now() : null
        );
    }
    
    /**
     * Increment progress.
     * @return New Progress with incremented value and whether this caused completion
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
        
        return new ProgressResult(newProgress, nowComplete && !completed);
    }
    
    /**
     * Set absolute progress.
     */
    public Progress set(int value) {
        int newCurrent = Math.max(0, Math.min(value, required));
        boolean nowComplete = newCurrent >= required;
        return new Progress(
            newCurrent,
            required,
            nowComplete,
            nowComplete ? Instant.now() : completedAt
        );
    }
    
    /**
     * Reset progress.
     */
    public Progress reset() {
        return new Progress(0, required, false, null);
    }
    
    /**
     * Get completion percentage (0.0 to 1.0).
     */
    public float percentage() {
        if (required == 0) return 1.0f;
        return (float) current / required;
    }
    
    /**
     * Get remaining count.
     */
    public int remaining() {
        return Math.max(0, required - current);
    }
    
    /**
     * Format as string like "5/10" or "10/10 ✓".
     */
    public String format() {
        return completed 
            ? String.format("%d/%d ✓", current, required)
            : String.format("%d/%d", current, required);
    }
}

/**
 * Result of a progress increment operation.
 */
public record ProgressResult(
    Progress progress,
    boolean justCompleted
) {}
```

---

## CL-004: Configuration Serialization

```java
package com.lordofthetales.framework.core.config;

import java.util.*;

/**
 * Abstraction over configuration sections.
 * Works with YAML, JSON, or any hierarchical config.
 */
public interface ConfigSection {
    
    // === Primitive Getters ===
    
    String getString(String path);
    String getString(String path, String defaultValue);
    
    int getInt(String path);
    int getInt(String path, int defaultValue);
    
    long getLong(String path);
    long getLong(String path, long defaultValue);
    
    double getDouble(String path);
    double getDouble(String path, double defaultValue);
    
    boolean getBoolean(String path);
    boolean getBoolean(String path, boolean defaultValue);
    
    // === Complex Types ===
    
    List<String> getStringList(String path);
    List<Integer> getIntList(String path);
    
    ConfigSection getSection(String path);
    List<ConfigSection> getSectionList(String path);
    
    Set<String> getKeys();
    Set<String> getKeys(boolean deep);
    
    boolean contains(String path);
    
    // === Type Conversion ===
    
    <T> T get(String path, Class<T> type);
    <T> T get(String path, Class<T> type, T defaultValue);
    
    <T> Optional<T> getOptional(String path, Class<T> type);
}

/**
 * Serializer for converting objects to/from config.
 */
public interface ConfigSerializer<T> {
    
    /**
     * Deserialize from config section.
     */
    T deserialize(ConfigSection config);
    
    /**
     * Serialize to config section.
     */
    void serialize(T object, ConfigSection config);
    
    /**
     * Validate config before deserialization.
     */
    default List<String> validate(ConfigSection config) {
        return List.of();
    }
}

/**
 * Registry for serializers by type.
 */
public class SerializerRegistry {
    
    private final Map<String, ConfigSerializer<?>> serializers = new HashMap<>();
    
    public <T> void register(String typeId, ConfigSerializer<T> serializer) {
        serializers.put(typeId, serializer);
    }
    
    @SuppressWarnings("unchecked")
    public <T> ConfigSerializer<T> get(String typeId) {
        return (ConfigSerializer<T>) serializers.get(typeId);
    }
    
    public <T> T deserialize(ConfigSection config) {
        String type = config.getString("type");
        if (type == null) {
            throw new ConfigException("Missing 'type' field");
        }
        
        ConfigSerializer<T> serializer = get(type);
        if (serializer == null) {
            throw new ConfigException("Unknown type: " + type);
        }
        
        List<String> errors = serializer.validate(config);
        if (!errors.isEmpty()) {
            throw new ConfigValidationException(type, errors);
        }
        
        return serializer.deserialize(config);
    }
}
```

---

## CL-005: Execution Tracing

```java
package com.lordofthetales.framework.core.debug;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Traces execution of operations for debugging.
 * Useful for understanding why conditions evaluated a certain way,
 * or why loot tables generated specific results.
 */
public class ExecutionTrace {
    
    private final String operationName;
    private final Instant startTime;
    private Instant endTime;
    private boolean success;
    private Object result;
    private String description;
    private final List<ExecutionTrace> children;
    private final Map<String, Object> metadata;
    
    public ExecutionTrace(String operationName) {
        this.operationName = operationName;
        this.startTime = Instant.now();
        this.children = new ArrayList<>();
        this.metadata = new LinkedHashMap<>();
    }
    
    public ExecutionTrace end(boolean success) {
        this.endTime = Instant.now();
        this.success = success;
        return this;
    }
    
    public ExecutionTrace end(boolean success, Object result) {
        this.result = result;
        return end(success);
    }
    
    public ExecutionTrace description(String description) {
        this.description = description;
        return this;
    }
    
    public ExecutionTrace addChild(ExecutionTrace child) {
        children.add(child);
        return this;
    }
    
    public ExecutionTrace metadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }
    
    public Duration getDuration() {
        if (endTime == null) return Duration.ZERO;
        return Duration.between(startTime, endTime);
    }
    
    /**
     * Format as pretty tree string.
     */
    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        toPrettyString(sb, "", true);
        return sb.toString();
    }
    
    private void toPrettyString(StringBuilder sb, String prefix, boolean isLast) {
        String icon = success ? "✓" : "✗";
        String duration = formatDuration(getDuration());
        
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(icon).append(" ").append(operationName);
        sb.append(" (").append(duration).append(")");
        
        if (description != null) {
            sb.append(" - ").append(description);
        }
        
        if (result != null) {
            sb.append(" → ").append(result);
        }
        
        sb.append("\n");
        
        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < children.size(); i++) {
            children.get(i).toPrettyString(sb, childPrefix, i == children.size() - 1);
        }
    }
    
    private String formatDuration(Duration duration) {
        long nanos = duration.toNanos();
        if (nanos < 1_000) {
            return nanos + "ns";
        } else if (nanos < 1_000_000) {
            return String.format("%.1fμs", nanos / 1000.0);
        } else if (nanos < 1_000_000_000) {
            return String.format("%.2fms", nanos / 1_000_000.0);
        } else {
            return String.format("%.2fs", nanos / 1_000_000_000.0);
        }
    }
    
    /**
     * Convert to structured map for logging/serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("operation", operationName);
        map.put("success", success);
        map.put("durationNanos", getDuration().toNanos());
        
        if (description != null) map.put("description", description);
        if (result != null) map.put("result", result.toString());
        if (!metadata.isEmpty()) map.put("metadata", metadata);
        
        if (!children.isEmpty()) {
            map.put("children", children.stream().map(ExecutionTrace::toMap).toList());
        }
        
        return map;
    }
}
```

---

## CL-006: Result Types

```java
package com.lordofthetales.framework.core.result;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents either a successful result or a failure with message.
 * Inspired by Rust's Result type.
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {
    
    boolean isSuccess();
    boolean isFailure();
    
    Optional<T> getValue();
    Optional<String> getError();
    
    <U> Result<U> map(Function<T, U> mapper);
    <U> Result<U> flatMap(Function<T, Result<U>> mapper);
    
    T orElse(T defaultValue);
    T orElseThrow();
    
    void ifSuccess(Consumer<T> action);
    void ifFailure(Consumer<String> action);
    
    // === Factory Methods ===
    
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }
    
    static <T> Result<T> failure(String message) {
        return new Failure<>(message);
    }
    
    static <T> Result<T> failure(String format, Object... args) {
        return new Failure<>(String.format(format, args));
    }
    
    // === Implementations ===
    
    record Success<T>(T value) implements Result<T> {
        @Override public boolean isSuccess() { return true; }
        @Override public boolean isFailure() { return false; }
        @Override public Optional<T> getValue() { return Optional.of(value); }
        @Override public Optional<String> getError() { return Optional.empty(); }
        
        @Override
        public <U> Result<U> map(Function<T, U> mapper) {
            return new Success<>(mapper.apply(value));
        }
        
        @Override
        public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
            return mapper.apply(value);
        }
        
        @Override public T orElse(T defaultValue) { return value; }
        @Override public T orElseThrow() { return value; }
        
        @Override
        public void ifSuccess(Consumer<T> action) { action.accept(value); }
        @Override
        public void ifFailure(Consumer<String> action) { /* no-op */ }
    }
    
    record Failure<T>(String message) implements Result<T> {
        @Override public boolean isSuccess() { return false; }
        @Override public boolean isFailure() { return true; }
        @Override public Optional<T> getValue() { return Optional.empty(); }
        @Override public Optional<String> getError() { return Optional.of(message); }
        
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
        
        @Override public T orElse(T defaultValue) { return defaultValue; }
        @Override public T orElseThrow() { 
            throw new ResultException(message); 
        }
        
        @Override
        public void ifSuccess(Consumer<T> action) { /* no-op */ }
        @Override
        public void ifFailure(Consumer<String> action) { action.accept(message); }
    }
}
```

---

## Module Structure

```
sf-core-lib/
├── pom.xml
├── src/main/java/com/lordofthetales/framework/core/
│   ├── random/
│   │   ├── WeightedSelector.java
│   │   ├── WeightedEntry.java
│   │   └── DefaultWeightedSelector.java
│   ├── ratelimit/
│   │   ├── RateLimiter.java
│   │   ├── RateLimitResult.java
│   │   ├── RateLimitStatus.java
│   │   └── ConfigurableRateLimiter.java
│   ├── progress/
│   │   ├── Progress.java
│   │   └── ProgressResult.java
│   ├── config/
│   │   ├── ConfigSection.java
│   │   ├── ConfigSerializer.java
│   │   ├── SerializerRegistry.java
│   │   └── ConfigException.java
│   ├── debug/
│   │   └── ExecutionTrace.java
│   ├── result/
│   │   ├── Result.java
│   │   └── ResultException.java
│   └── validation/
│       ├── Validator.java
│       └── Validators.java
└── src/test/java/
    └── (comprehensive unit tests)
```

---

## Usage Examples

### Weighted Selection
```java
List<WeightedEntry<String>> loot = List.of(
    WeightedEntry.of("gold", 100),
    WeightedEntry.of("silver", 50),
    WeightedEntry.of("diamond", 5)
);

WeightedSelector<String> selector = DefaultWeightedSelector.getInstance();
String drop = selector.selectOne(loot, ThreadLocalRandom.current())
    .orElse("nothing");
```

### Rate Limiting
```java
RateLimiter<UUID> questLimiter = ConfigurableRateLimiter.<UUID>builder()
    .cooldown(Duration.ofMinutes(5))
    .hourlyLimit(10)
    .dailyLimit(50)
    .build();

if (questLimiter.tryAcquire(playerId)) {
    generateQuest();
} else {
    RateLimitResult result = questLimiter.checkLimit(playerId);
    player.sendMessage("Try again in " + result.retryAfter());
}
```

### Progress Tracking
```java
Progress killProgress = Progress.of(10); // 0/10 kills

ProgressResult result = killProgress.increment(1);
if (result.justCompleted()) {
    rewardPlayer();
}

player.sendMessage("Progress: " + result.progress().format());
```

---

## Related Documents

- [SF-01: Accessor API](SF-01-accessor-api.md) - Uses these utilities
- [SF-03: Condition Framework](SF-03-condition-framework.md) - Uses ExecutionTrace
- [SF-04: Objective Tracking](SF-04-objective-tracking-lib.md) - Uses Progress

## Design

### Component Context

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

title Component Diagram for SF-02: Core Library

Container_Boundary(framework, "SF-02: Core Library") {
}

@enduml
```
