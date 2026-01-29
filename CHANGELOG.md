# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.1.0] - 2026-01-30

### Added
- **ConfigValidator**: New fluent validation API for configuration files (spec CL-004)
  - Supports required fields, optional fields with custom validators, pattern matching, range validation
  - Supports nested section validation and list element validation
  - Integration with `Result<T, E>` for functional error handling
  - `formatErrors()` method for human-readable error messages
- **ConfigWatcher**: New hot-reload configuration watching with `WatchService` (spec CL-004)
  - Debouncing support to prevent callback spam
  - Thread-safe file registration and event handling
  - Graceful shutdown with `AutoCloseable` support
- **DataValueConverterTest**: 24 unit tests for DataValueConverter
- **ConfigValidatorTest**: 30+ unit tests for ConfigValidator rules and integration
- **ConfigWatcherTest**: 17 unit tests for file watching and event handling

### Fixed
- V-001: Fixed `DataValueConverter.toRawValue()` returning `null` for unknown types (now throws `IllegalArgumentException`)

### Changed
- Test coverage increased from 42 tests to 166 tests (395% increase)

## [2.0.0] - 2026-01-29

### Added
- **DataValueConverter**: New utility class for converting between raw `Object` maps and type-safe `DataValue` maps
- **ConfigSerializer.serializeTyped()**: New type-safe serialization method returning `Map<String, DataValue>`
- **ExecutionTrace.getMetadataTyped()**: New type-safe metadata accessor
- **ExecutionTrace.toMapTyped()**: New type-safe map serialization
- **ExecutionTrace.metadata(String, DataValue)**: New type-safe metadata setter overload

### Changed
- **BREAKING**: `ConfigSerializer.getTypeId()` now returns `Optional<String>` instead of nullable `String`

### Deprecated
- `ConfigSerializer.serialize()` - Use `serializeTyped()` for type-safe serialization
- `ExecutionTrace.getMetadata()` - Use `getMetadataTyped()` for type-safe access
- `ExecutionTrace.toMap()` - Use `toMapTyped()` for type-safe serialization

### Removed
- **CoreLibPlugin.java** - Violated ZERO Hytale rule; plugin entry points belong in adapter layer

### Fixed
- V-001: Removed Hytale API leak (`CoreLibPlugin.java` importing `com.hypixel.hytale.*`)
- V-002: Fixed `ConfigSerializer.getTypeId()` returning `null` (now returns `Optional.empty()`)

### Security

## [1.0.0] - 2026-01-25

### Added
- Initial release
- Core functionality implemented
- Documentation and examples
- Build system configured

[Unreleased]: https://github.com/Argonath-Systems/02-framework-core/compare/v2.1.0...HEAD
[2.1.0]: https://github.com/Argonath-Systems/02-framework-core/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/Argonath-Systems/02-framework-core/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/Argonath-Systems/02-framework-core/releases/tag/v1.0.0
