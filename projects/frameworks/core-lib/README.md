# SF-02: Core Library

> Common utilities used across all standalone mods and framework libraries. **ZERO Hytale imports.**

---

## 📋 Overview

| Attribute | Value |
|-----------|-------|
| **Module ID** | `sf-core-lib` |
| **Maven Artifact** | `com.lordofthetales.framework:core-lib` |
| **License** | MIT |
| **Dependencies** | SF-01 Accessor API |
| **Java Version** | 25+ |

### Value Proposition

- **Reusable**: Common utilities for all mods
- **Tested**: Comprehensive unit test coverage
- **Zero Platform**: No Hytale dependencies

---

## 🏗️ C4 Architecture

### C2: Container View

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         SF-02: CORE LIBRARY                                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         core.random                                    │ │
│  │                                                                        │ │
│  │  ┌─────────────────────┐  ┌─────────────────────────────────────────┐ │ │
│  │  │ WeightedSelector<T> │  │ WeightedEntry<T>                        │ │ │
│  │  │ (Interface)         │  │ (Record: item, weight)                  │ │ │
│  │  │                     │  └─────────────────────────────────────────┘ │ │
│  │  │ + selectOne()       │                                              │ │
│  │  │ + selectMultiple()  │  ┌─────────────────────────────────────────┐ │ │
│  │  │ + selectOneWhere()  │  │ DefaultWeightedSelector                 │ │ │
│  │  └─────────────────────┘  │ (Implementation)                        │ │ │
│  │                           └─────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         core.ratelimit                                 │ │
│  │                                                                        │ │
│  │  ┌─────────────────────┐  ┌─────────────────────────────────────────┐ │ │
│  │  │ RateLimiter<K>      │  │ RateLimitConfig                         │ │ │
│  │  │ (Interface)         │  │ (Record: burstLimit, hourly, daily)     │ │ │
│  │  │                     │  └─────────────────────────────────────────┘ │ │
│  │  │ + checkLimit()      │                                              │ │
│  │  │ + recordAction()    │  ┌─────────────────────────────────────────┐ │ │
│  │  │ + getRemainingTime()│  │ InMemoryRateLimiter                     │ │ │
│  │  └─────────────────────┘  │ (Implementation)                        │ │ │
│  │                           └─────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         core.progress                                  │ │
│  │                                                                        │ │
│  │  ┌─────────────────────┐  ┌─────────────────────────────────────────┐ │ │
│  │  │ ProgressTracker     │  │ ProgressState                           │ │ │
│  │  │ (Interface)         │  │ (Record: current, target, percentage)   │ │ │
│  │  └─────────────────────┘  └─────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         core.config                                    │ │
│  │                                                                        │ │
│  │  ┌─────────────────────┐  ┌─────────────────────────────────────────┐ │ │
│  │  │ ConfigSerializer    │  │ YamlConfigSerializer                    │ │ │
│  │  │ (Interface)         │  │ JsonConfigSerializer                    │ │ │
│  │  └─────────────────────┘  └─────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         core.result                                    │ │
│  │                                                                        │ │
│  │  ┌──────────────────────────────────────────────────────────────────┐ │ │
│  │  │ Result<T, E>  (sealed interface)                                 │ │ │
│  │  │   ├── Success<T> (record)                                        │ │ │
│  │  │   └── Failure<E> (record)                                        │ │ │
│  │  │                                                                  │ │ │
│  │  │ + map(Function<T,U>): Result<U,E>                                │ │ │
│  │  │ + flatMap(Function<T,Result<U,E>>): Result<U,E>                  │ │ │
│  │  │ + orElse(T): T                                                   │ │ │
│  │  │ + orElseThrow(): T                                               │ │ │
│  │  └──────────────────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │  core.debug                │  core.validation                         │ │
│  │  ExecutionTrace            │  Validators                              │ │
│  │  (Timing & logging)        │  (Input validation utilities)            │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Features

### CL-001: Weighted Random Selection

```java
WeightedSelector<String> selector = DefaultWeightedSelector.getInstance();

List<WeightedEntry<String>> lootPool = List.of(
    WeightedEntry.of("common_sword", 100.0),
    WeightedEntry.of("rare_sword", 20.0),
    WeightedEntry.of("legendary_sword", 1.0)
);

// Select one item
Optional<String> drop = selector.selectOne(lootPool, random);

// Select 3 items (no duplicates)
List<String> drops = selector.selectMultiple(lootPool, 3, false, random);

// Select with filter
Optional<String> rareDrop = selector.selectOneWhere(
    lootPool, 
    item -> !item.startsWith("common"),
    random
);
```

### CL-002: Rate Limiting

```java
RateLimiter<UUID> limiter = new InMemoryRateLimiter<>(
    RateLimitConfig.of()
        .burstLimit(5, Duration.ofSeconds(10))  // 5 per 10 sec
        .hourlyLimit(100)                        // 100 per hour
        .dailyLimit(500)                         // 500 per day
        .build()
);

UUID playerId = player.getId();

if (limiter.checkLimit(playerId).isAllowed()) {
    limiter.recordAction(playerId);
    // Process action
} else {
    Duration wait = limiter.getRemainingTime(playerId);
    player.sendMessage("Wait " + wait.getSeconds() + "s");
}
```

### CL-003: Progress Tracking

```java
ProgressTracker tracker = new DefaultProgressTracker();

tracker.setTarget(playerId, "kill_zombies", 10);
tracker.increment(playerId, "kill_zombies", 1);

ProgressState state = tracker.getProgress(playerId, "kill_zombies");
// state.current() = 1
// state.target() = 10
// state.percentage() = 0.1
// state.isComplete() = false
```

### CL-004: Result Type

```java
Result<PlayerData, String> result = loadPlayer(playerId);

// Pattern matching (Java 21+)
String message = switch (result) {
    case Success(var player) -> "Loaded: " + player.name();
    case Failure(var error) -> "Error: " + error;
};

// Functional style
result
    .map(player -> player.name())
    .flatMap(name -> validateName(name))
    .orElse("Unknown");
```

### CL-005: Execution Trace (Debug)

```java
ExecutionTrace trace = ExecutionTrace.start("QuestComplete");

trace.step("LoadQuest");
Quest quest = loadQuest(questId);

trace.step("ValidateConditions");
boolean valid = validateConditions(quest);

trace.step("GrantRewards");
grantRewards(player, quest.rewards());

trace.end();
trace.log();  // Prints timing breakdown

// Output:
// [QuestComplete] Total: 45ms
//   LoadQuest: 12ms (26%)
//   ValidateConditions: 8ms (18%)
//   GrantRewards: 25ms (56%)
```

---

## 📦 Package Structure

```
com.lordofthetales.framework.core/
├── random/
│   ├── WeightedSelector.java       # Selection interface
│   ├── WeightedEntry.java          # Entry record
│   └── DefaultWeightedSelector.java # Implementation
│
├── ratelimit/
│   ├── RateLimiter.java            # Limiter interface
│   ├── RateLimitConfig.java        # Configuration record
│   ├── RateLimitResult.java        # Check result
│   └── InMemoryRateLimiter.java    # Implementation
│
├── progress/
│   ├── ProgressTracker.java        # Tracker interface
│   ├── ProgressState.java          # State record
│   └── DefaultProgressTracker.java # Implementation
│
├── config/
│   ├── ConfigSerializer.java       # Serialization interface
│   ├── YamlConfigSerializer.java   # YAML implementation
│   └── JsonConfigSerializer.java   # JSON implementation
│
├── result/
│   ├── Result.java                 # Sealed interface
│   ├── Success.java                # Success record
│   └── Failure.java                # Failure record
│
├── debug/
│   └── ExecutionTrace.java         # Timing utility
│
└── validation/
    └── Validators.java             # Validation utilities
```

---

## 🔧 Usage

### Maven Coordinates

```xml
<dependency>
    <groupId>com.lordofthetales.framework</groupId>
    <artifactId>core-lib</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Quick Start

```bash
just build
just test
just install
```

---

## 📋 Implementation Checklist

- [ ] `WeightedSelector<T>` interface
- [ ] `WeightedEntry<T>` record
- [ ] `DefaultWeightedSelector<T>` implementation
- [ ] `RateLimiter<K>` interface
- [ ] `RateLimitConfig` record
- [ ] `InMemoryRateLimiter<K>` implementation
- [ ] `ProgressTracker` interface
- [ ] `ProgressState` record
- [ ] `ConfigSerializer` interface
- [ ] `YamlConfigSerializer` implementation
- [ ] `Result<T,E>` sealed interface
- [ ] `Success<T>` record
- [ ] `Failure<E>` record
- [ ] `ExecutionTrace` utility
- [ ] `Validators` utility class

---

## 📚 Related Documentation

- [SF-02 Spec](../../specs/standalone-framework/SF-02-core-lib.md) - Full specification
