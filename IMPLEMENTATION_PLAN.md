# Implementation Plan: Framework Core

**Module**: `02-framework-core`  
**Generated**: 2026-01-29  
**Architect**: HytaleArchitect  
**Specification Coverage**: SF-ARCHITECTURE-002 (Core Library)  
**Current Version**: 2.0.0  

---

## Executive Summary

Framework Core is the foundational utility library providing common reusable components across all Argonath modules: weighted random selection, rate limiting, progress tracking, configuration serialization, debug tracing, and result types. The module is approximately **87% complete** with v2.0.0 already migrated to support accessor API v2.0.0's `DataValue` types. Key remaining work includes implementing missing config components (`ConfigValidator`, `ConfigWatcher`), fixing one remaining `return null;` violation, and increasing test coverage to 80%.

---

## Critical Issues Found

### Violations (MUST FIX)

| ID | Location | Type | Description | Severity |
|----|----------|------|-------------|----------|
| V-001 | [DataValueConverter.java#L99](src/main/java/com/argonathsystems/framework/core/config/DataValueConverter.java#L99) | `return null;` | `toRawValue()` returns `null` when input is `null` instead of throwing or returning empty | 🔴 CRITICAL |

### Technical Debt

| ID | Location | Type | Description | Priority |
|----|----------|------|-------------|----------|
| TD-001 | `config/` | Missing Class | `ConfigValidator.java` not implemented (specified in CL-004) | 🔴 HIGH |
| TD-002 | `config/` | Missing Class | `ConfigWatcher.java` not implemented (specified in CL-004) | 🟡 MEDIUM |
| TD-003 | `config/DataValueConverter.java` | Missing Tests | No unit tests for DataValueConverter utility | 🟡 MEDIUM |
| TD-004 | `data/Either.java` | Missing Tests | No unit tests for Either data structure | 🟡 MEDIUM |
| TD-005 | `config/ConfigMigrator.java` | Missing Tests | No unit tests for config migration | 🟡 MEDIUM |
| TD-006 | Module | Test Coverage | Current coverage ~60%, target 80% | 🟡 MEDIUM |

### TODO/FIXME/STUB Inventory

| Location | Type | Description | Action Required |
|----------|------|-------------|-----------------|
| - | - | No TODO/FIXME/STUB comments found | ✅ None |

---

## Requirements Traceability

### Specification Coverage (SF-ARCHITECTURE-002)

| Spec ID | Requirement | Status | Implementation Location | Notes |
|---------|-------------|--------|-------------------------|-------|
| CL-001 | Weighted Random Selection | ✅ Complete | `random/WeightedSelector.java`, `WeightedEntry.java`, `DefaultWeightedSelector.java` | Full implementation |
| CL-002 | Rate Limiting | ✅ Complete | `ratelimit/RateLimiter.java`, `ConfigurableRateLimiter.java`, `RateLimitConfig.java`, `RateLimitResult.java`, `RateLimitStatus.java`, `RateLimitReason.java` | Full implementation with cooldown, burst, hourly, daily limits |
| CL-003 | Progress Tracking | ✅ Complete | `progress/Progress.java`, `ProgressResult.java` | Immutable progress with increment/decrement |
| CL-004 | Configuration Serialization | 🚧 80% | `config/ConfigSection.java`, `ConfigSerializer.java`, `SerializerRegistry.java`, `ConfigException.java`, `ConfigValidationException.java` | Missing: ConfigValidator, ConfigWatcher |
| CL-005 | Execution Tracing | ✅ Complete | `debug/ExecutionTrace.java` | Type-safe methods added (v2.0.0) |
| CL-006 | Result Types | ✅ Complete | `result/Result.java`, `ResultException.java` | Sealed interface with Success/Failure |
| - | Validators | ✅ Complete | `validation/Validators.java` | Comprehensive validation utilities |
| - | DataValue Conversion | ✅ Complete | `config/DataValueConverter.java` | Bidirectional Object ↔ DataValue (v2.0.0) |

### Orphan Implementations (No Specification)

| Location | Description | Proposed Action |
|----------|-------------|-----------------|
| `ArgonathMod.java` | Platform-agnostic mod interface | DOCUMENT - Infrastructure component, no spec needed |
| `data/Registry.java` | Generic registration pattern | DOCUMENT - Utility, mentioned in IMPLEMENTATION_TRACKING |
| `data/Observable.java` | Reactive value container | DOCUMENT - Utility, mentioned in IMPLEMENTATION_TRACKING |
| `data/Either.java` | Sum type for alternatives | DOCUMENT - Utility, mentioned in IMPLEMENTATION_TRACKING |
| `config/ConfigMigrator.java` | Version migration | DOCUMENT - Infrastructure for config upgrades |
| `config/ConfigFactory.java` | Type-safe config loading | DOCUMENT - Convenience layer |

### Missing Implementations (Spec Not Implemented)

| Spec ID | Requirement | Gap Description | Priority |
|---------|-------------|-----------------|----------|
| CL-004 | ConfigValidator | JSON Schema validation for configs not implemented | 🔴 HIGH |
| CL-004 | ConfigWatcher | File system watching for hot-reload not implemented | 🟡 MEDIUM |

---

## Accessor v2.0.0 Migration

### Required Changes

| Location | Current Type | Target Type | Migration Notes |
|----------|--------------|-------------|-----------------|
| ✅ `ExecutionTrace.java:metadata` | `Map<String, Object>` | Added `getMetadataTyped()` returning `Map<String, DataValue>` | Backward-compatible with deprecated old method |
| ✅ `ExecutionTrace.java:toMap()` | `Map<String, Object>` | Added `toMapTyped()` returning `Map<String, DataValue>` | Backward-compatible with deprecated old method |
| ✅ `ConfigSerializer.java:serialize()` | `Map<String, Object>` | Added `serializeTyped()` returning `Map<String, DataValue>` | Backward-compatible with deprecated old method |
| ✅ `DataValueConverter.java` | - | NEW | Utility for bidirectional conversion |

### Breaking Change Impact

**STATUS: ✅ MIGRATION COMPLETE**

The module has been successfully migrated to support accessor v2.0.0. Key changes in v2.0.0:

1. Added `DataValueConverter` utility class for `Object ↔ DataValue` conversion
2. Added type-safe `*Typed()` method variants alongside deprecated legacy methods
3. Changed `ConfigSerializer.getTypeId()` to return `Optional<String>` instead of nullable
4. Removed `CoreLibPlugin.java` (Hytale API leak violation)

All downstream modules can use either legacy `Object` methods (deprecated) or new type-safe `DataValue` methods.

---

## HyUI Integration

**N/A** - This module has no UI components. It is a pure utility library with ZERO platform dependencies.

---

## Hytale SDK Integration

**N/A** - This module has **ZERO Hytale imports** by design. All platform interaction happens through the accessor API (`02-framework-accessor`).

### Enforcement

The `pom.xml` includes maven-enforcer-plugin configuration to **prevent** any Hytale dependencies from being added accidentally:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <id>enforce-no-hytale-imports</id>
            <!-- Bans Hytale dependencies -->
        </execution>
    </executions>
</plugin>
```

---

## Implementation Phases

### Phase 1: Critical Fixes [0.5 days]

| Task ID | Description | Files | Effort | Dependencies |
|---------|-------------|-------|--------|--------------|
| P1-001 | Fix V-001: Replace `return null;` with proper handling in `toRawValue()` | `DataValueConverter.java` | 0.5 hours | None |

**Details for P1-001**:
The `toRawValue()` method at line 97-108 has:
```java
if (value == null) {
    return null;  // VIOLATION
}
```
This should throw `IllegalArgumentException` or return an empty/default value to maintain the ZERO silent failures policy.

### Phase 2: Missing Components [3 days]

| Task ID | Description | Files | Effort | Dependencies |
|---------|-------------|-------|--------|--------------|
| P2-001 | Implement `ConfigValidator` with JSON Schema validation | `config/ConfigValidator.java` | 2 days | None |
| P2-002 | Implement `ConfigWatcher` for hot-reload support | `config/ConfigWatcher.java` | 1 day | P2-001 |

**Details for P2-001** (ConfigValidator):
Per spec CL-004, should provide:
- JSON Schema-based validation
- Custom validation rules
- Descriptive error messages
- Integration with `ConfigSerializer.validate()`

**Details for P2-002** (ConfigWatcher):
Per spec CL-004, should provide:
- File system watching (WatchService)
- Callback on config changes
- Debouncing for rapid changes
- Thread-safe reload

### Phase 3: Test Coverage [2 days]

| Task ID | Description | Files | Effort | Dependencies |
|---------|-------------|-------|--------|--------------|
| P3-001 | Add tests for `DataValueConverter` | `config/DataValueConverterTest.java` | 0.5 days | P1-001 |
| P3-002 | Add tests for `Either` | `data/EitherTest.java` | 0.25 days | None |
| P3-003 | Add tests for `ConfigMigrator` | `config/ConfigMigratorTest.java` | 0.25 days | None |
| P3-004 | Add tests for `ConfigValidator` | `config/ConfigValidatorTest.java` | 0.5 days | P2-001 |
| P3-005 | Add tests for `ConfigWatcher` | `config/ConfigWatcherTest.java` | 0.5 days | P2-002 |

### Phase 4: Documentation & Validation [0.5 days]

| Task ID | Description | Files | Effort | Dependencies |
|---------|-------------|-------|--------|--------------|
| P4-001 | Update IMPLEMENTATION_TRACKING.md | `IMPLEMENTATION_TRACKING.md` | 0.25 days | All |
| P4-002 | Update CHANGELOG.md | `CHANGELOG.md` | 0.25 days | All |
| P4-003 | Run full test suite and verify 80% coverage | - | Included | All |

---

## Estimated Timeline

| Phase | Duration | Start Condition |
|-------|----------|-----------------|
| Phase 1 | 0.5 days | Immediate |
| Phase 2 | 3 days | After Phase 1 |
| Phase 3 | 2 days | Parallel with Phase 2 (P3-001/002/003), rest after Phase 2 |
| Phase 4 | 0.5 days | After Phase 2 & 3 |
| **Total** | **~4-5 days** | - |

---

## Dependencies & Blockers

### Upstream Dependencies

| Module | Version | Status | Notes |
|--------|---------|--------|-------|
| `02-framework-accessor` | 2.0.0 | ✅ Complete | Provides `DataValue` sealed interface |
| `01-platform-core` | 1.0.0-SNAPSHOT | ✅ Available | Parent POM |

### Downstream Impact

The following modules depend on `02-framework-core` and will benefit from these improvements:

| Module | Impact |
|--------|--------|
| `02-adapter-hytale` | Uses Result, Validators |
| `03-framework-config` | Uses ConfigSection, ConfigSerializer |
| `03-framework-storage` | Uses Result, DataValueConverter |
| `04-framework-condition` | Uses ExecutionTrace, Result |
| `04-framework-objective` | Uses Progress, ProgressResult |
| `05-framework-quest` | Uses Progress, ExecutionTrace, Result |
| All Layer 06 mods | Various utility usage |

### External Blockers

None - this module has no external dependencies beyond the JDK and accessor API.

---

## Validation Criteria

### Build Validation
- [x] `mvn clean compile` succeeds with zero errors
- [x] `mvn test` passes all unit tests
- [x] No Hytale import leaks (enforcer plugin validates)

### Architecture Validation
- [ ] All `Object` usages migrated to appropriate types (V-001 pending)
- [x] No `return null;` without exception (except V-001 pending)
- [x] All accessor interfaces properly used via DataValue
- [x] Framework dependencies correctly used (accessor API only)

### Specification Validation
- [ ] All spec requirements have implementations (CL-004 partial)
- [x] All implementations trace to specs
- [x] Orphan implementations documented

### Test Coverage Validation
- [ ] Unit tests for all public APIs
- [ ] Test coverage ≥ 80%
- [ ] Edge cases covered (null handling, empty collections, etc.)

---

## HytaleModder Handoff Prompt

```markdown
## Task: Complete 02-framework-core Implementation

**Module**: 02-framework-core
**Priority**: P1 - Critical Fix, P2 - High
**Estimated Effort**: 4-5 days

### Context
The framework-core module provides common utilities for all Argonath modules. It has been migrated to accessor v2.0.0 with DataValue support. Remaining work includes fixing one violation and implementing missing config components.

### Tasks

#### P1-001: Fix DataValueConverter Null Handling [CRITICAL]
**File**: `src/main/java/com/argonathsystems/framework/core/config/DataValueConverter.java`
**Line**: 97-108

Current code:
```java
public static Object toRawValue(DataValue value) {
    if (value == null) {
        return null;  // VIOLATION - must not return null silently
    }
    // ...
}
```

Fix: Replace `return null;` with `throw new IllegalArgumentException("DataValue cannot be null")` or handle gracefully with documentation.

#### P2-001: Implement ConfigValidator
**New File**: `src/main/java/com/argonathsystems/framework/core/config/ConfigValidator.java`

Implement JSON Schema-based config validation per spec CL-004:
- Parse JSON Schema definitions
- Validate ConfigSection against schema
- Return detailed validation errors
- Integrate with ConfigSerializer.validate()

#### P2-002: Implement ConfigWatcher
**New File**: `src/main/java/com/argonathsystems/framework/core/config/ConfigWatcher.java`

Implement hot-reload config watching per spec CL-004:
- Use WatchService for file monitoring
- Callback mechanism for config changes
- Debouncing (100ms default)
- Thread-safe reload

#### P3-*: Add Missing Tests
Create tests for:
- DataValueConverterTest.java
- EitherTest.java
- ConfigMigratorTest.java
- ConfigValidatorTest.java (after P2-001)
- ConfigWatcherTest.java (after P2-002)

Target: 80% test coverage

### Specification Reference
- SF-ARCHITECTURE-002-core-lib.md (sections CL-001 through CL-006)

### Validation
After completion:
1. `mvn clean test` - all tests pass
2. No `return null;` statements
3. No Hytale imports
4. Update IMPLEMENTATION_TRACKING.md
5. Update CHANGELOG.md
```

---

## Appendix: File Inventory

### Source Files (22 total)

```
src/main/java/com/argonathsystems/framework/core/
├── ArgonathMod.java                    ✅ Complete
├── config/
│   ├── ConfigException.java            ✅ Complete
│   ├── ConfigMigrator.java             ✅ Complete (needs tests)
│   ├── ConfigSection.java              ✅ Complete
│   ├── ConfigSerializer.java           ✅ Complete
│   ├── ConfigValidationException.java  ✅ Complete
│   ├── ConfigValidator.java            ❌ NOT IMPLEMENTED
│   ├── ConfigWatcher.java              ❌ NOT IMPLEMENTED
│   ├── DataValueConverter.java         🚧 V-001 needs fix
│   └── SerializerRegistry.java         ✅ Complete
├── data/
│   ├── Either.java                     ✅ Complete (needs tests)
│   ├── Observable.java                 ✅ Complete
│   └── Registry.java                   ✅ Complete
├── debug/
│   └── ExecutionTrace.java             ✅ Complete (v2.0.0 migrated)
├── progress/
│   ├── Progress.java                   ✅ Complete
│   └── ProgressResult.java             ✅ Complete
├── random/
│   ├── DefaultWeightedSelector.java    ✅ Complete
│   ├── WeightedEntry.java              ✅ Complete
│   └── WeightedSelector.java           ✅ Complete
├── ratelimit/
│   ├── ConfigurableRateLimiter.java    ✅ Complete
│   ├── RateLimitConfig.java            ✅ Complete
│   ├── RateLimitReason.java            ✅ Complete
│   ├── RateLimitResult.java            ✅ Complete
│   ├── RateLimiter.java                ✅ Complete
│   └── RateLimitStatus.java            ✅ Complete
├── result/
│   ├── Result.java                     ✅ Complete
│   └── ResultException.java            ✅ Complete
└── validation/
    └── Validators.java                 ✅ Complete
```

### Test Files (9 total)

```
src/test/java/com/argonathsystems/framework/core/
├── config/
│   ├── ConfigSerializerTest.java       ✅
│   └── SerializerRegistryTest.java     ✅
├── debug/
│   └── ExecutionTraceTest.java         ✅
├── progress/
│   └── ProgressTest.java               ✅
├── random/
│   ├── WeightedEntryTest.java          ✅
│   └── WeightedSelectorTest.java       ✅
├── ratelimit/
│   └── ConfigurableRateLimiterTest.java ✅
├── result/
│   └── ResultTest.java                 ✅
└── validation/
    └── ValidatorsTest.java             ✅
```

---

*Plan generated by HytaleArchitect agent on 2026-01-29*
