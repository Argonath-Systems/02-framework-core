package com.argonathsystems.framework.core.config;

import com.argonathsystems.framework.core.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ConfigValidator.
 */
@DisplayName("ConfigValidator")
class ConfigValidatorTest {

    private ConfigSection mockConfig;

    @BeforeEach
    void setUp() {
        mockConfig = mock(ConfigSection.class);
    }

    // =========================================================================
    // Required Field Tests
    // =========================================================================

    @Nested
    @DisplayName("require()")
    class RequiredFieldTests {

        @Test
        @DisplayName("passes when required field is present")
        void passesWhenFieldPresent() {
            when(mockConfig.contains("name")).thenReturn(true);
            when(mockConfig.get("name", String.class)).thenReturn("Test");

            ConfigValidator validator = ConfigValidator.builder()
                    .require("name", String.class)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("fails when required field is missing")
        void failsWhenFieldMissing() {
            when(mockConfig.contains("name")).thenReturn(false);

            ConfigValidator validator = ConfigValidator.builder()
                    .require("name", String.class)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0)).contains("name").contains("missing");
        }

        @Test
        @DisplayName("fails when required field is null")
        void failsWhenFieldNull() {
            when(mockConfig.contains("name")).thenReturn(true);
            when(mockConfig.get("name", String.class)).thenReturn(null);

            ConfigValidator validator = ConfigValidator.builder()
                    .require("name", String.class)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0)).contains("name").contains("null");
        }

        @Test
        @DisplayName("applies custom validator when present")
        void appliesCustomValidator() {
            when(mockConfig.contains("level")).thenReturn(true);
            when(mockConfig.get("level", Integer.class)).thenReturn(150);

            ConfigValidator validator = ConfigValidator.builder()
                    .require("level", Integer.class, v -> v > 100 ? "Level must be <= 100" : null)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).contains("level").contains("<= 100");
        }

        @Test
        @DisplayName("passes custom validation when valid")
        void passesCustomValidation() {
            when(mockConfig.contains("level")).thenReturn(true);
            when(mockConfig.get("level", Integer.class)).thenReturn(50);

            ConfigValidator validator = ConfigValidator.builder()
                    .require("level", Integer.class, v -> v > 100 ? "Level must be <= 100" : null)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isTrue();
        }
    }

    // =========================================================================
    // Optional Field Tests
    // =========================================================================

    @Nested
    @DisplayName("optional()")
    class OptionalFieldTests {

        @Test
        @DisplayName("passes when optional field is missing")
        void passesWhenFieldMissing() {
            when(mockConfig.contains("description")).thenReturn(false);

            ConfigValidator validator = ConfigValidator.builder()
                    .optional("description", String.class)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("passes when optional field is present and valid")
        void passesWhenFieldPresentAndValid() {
            when(mockConfig.contains("description")).thenReturn(true);
            when(mockConfig.get("description", String.class)).thenReturn("Some text");

            ConfigValidator validator = ConfigValidator.builder()
                    .optional("description", String.class)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("validates optional field when present")
        void validatesWhenPresent() {
            when(mockConfig.contains("score")).thenReturn(true);
            when(mockConfig.get("score", Integer.class)).thenReturn(-5);

            ConfigValidator validator = ConfigValidator.builder()
                    .optional("score", Integer.class, v -> v < 0 ? "Score cannot be negative" : null)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).contains("score").contains("negative");
        }
    }

    // =========================================================================
    // Pattern Tests
    // =========================================================================

    @Nested
    @DisplayName("requirePattern()")
    class PatternTests {

        @Test
        @DisplayName("passes when pattern matches")
        void passesWhenPatternMatches() {
            when(mockConfig.contains("id")).thenReturn(true);
            when(mockConfig.getString("id")).thenReturn("quest_001");

            ConfigValidator validator = ConfigValidator.builder()
                    .requirePattern("id", "^quest_\\d+$")
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("fails when pattern does not match")
        void failsWhenPatternDoesNotMatch() {
            when(mockConfig.contains("id")).thenReturn(true);
            when(mockConfig.getString("id")).thenReturn("invalid-id");

            ConfigValidator validator = ConfigValidator.builder()
                    .requirePattern("id", "^quest_\\d+$")
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).contains("id").contains("pattern");
        }
    }

    // =========================================================================
    // Range Tests
    // =========================================================================

    @Nested
    @DisplayName("requireRange()")
    class RangeTests {

        @Test
        @DisplayName("passes when value is in range")
        void passesWhenInRange() {
            when(mockConfig.contains("level")).thenReturn(true);
            when(mockConfig.getDouble("level")).thenReturn(50.0);

            ConfigValidator validator = ConfigValidator.builder()
                    .requireRange("level", 1, 100)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("fails when value is below range")
        void failsWhenBelowRange() {
            when(mockConfig.contains("level")).thenReturn(true);
            when(mockConfig.getDouble("level")).thenReturn(0.0);

            ConfigValidator validator = ConfigValidator.builder()
                    .requireRange("level", 1, 100)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).contains("level").contains("between");
        }

        @Test
        @DisplayName("fails when value is above range")
        void failsWhenAboveRange() {
            when(mockConfig.contains("level")).thenReturn(true);
            when(mockConfig.getDouble("level")).thenReturn(150.0);

            ConfigValidator validator = ConfigValidator.builder()
                    .requireRange("level", 1, 100)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
        }
    }

    // =========================================================================
    // OneOf / Enum Tests
    // =========================================================================

    @Nested
    @DisplayName("requireOneOf() / requireEnum()")
    class OneOfTests {

        @Test
        @DisplayName("passes when value is in allowed set")
        void passesWhenValueAllowed() {
            when(mockConfig.contains("status")).thenReturn(true);
            when(mockConfig.getString("status")).thenReturn("ACTIVE");

            ConfigValidator validator = ConfigValidator.builder()
                    .requireOneOf("status", Set.of("ACTIVE", "INACTIVE", "PENDING"))
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("fails when value is not in allowed set")
        void failsWhenValueNotAllowed() {
            when(mockConfig.contains("status")).thenReturn(true);
            when(mockConfig.getString("status")).thenReturn("UNKNOWN");

            ConfigValidator validator = ConfigValidator.builder()
                    .requireOneOf("status", Set.of("ACTIVE", "INACTIVE", "PENDING"))
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).contains("status").contains("one of");
        }

        enum TestStatus { ACTIVE, INACTIVE }

        @Test
        @DisplayName("requireEnum works with enum class")
        void requireEnumWorks() {
            when(mockConfig.contains("status")).thenReturn(true);
            when(mockConfig.getString("status")).thenReturn("ACTIVE");

            ConfigValidator validator = ConfigValidator.builder()
                    .requireEnum("status", TestStatus.class)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isTrue();
        }
    }

    // =========================================================================
    // Section Tests
    // =========================================================================

    @Nested
    @DisplayName("requireSection()")
    class SectionTests {

        @Test
        @DisplayName("passes when section exists")
        void passesWhenSectionExists() {
            ConfigSection nestedSection = mock(ConfigSection.class);
            when(mockConfig.getSection("settings")).thenReturn(nestedSection);

            ConfigValidator validator = ConfigValidator.builder()
                    .requireSection("settings")
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("fails when section is missing")
        void failsWhenSectionMissing() {
            when(mockConfig.getSection("settings")).thenReturn(null);

            ConfigValidator validator = ConfigValidator.builder()
                    .requireSection("settings")
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).contains("settings").contains("missing");
        }

        @Test
        @DisplayName("validates nested section with validator")
        void validatesNestedSection() {
            ConfigSection nestedSection = mock(ConfigSection.class);
            when(mockConfig.getSection("settings")).thenReturn(nestedSection);
            when(nestedSection.contains("enabled")).thenReturn(false);

            ConfigValidator nestedValidator = ConfigValidator.builder()
                    .require("enabled", Boolean.class)
                    .build();

            ConfigValidator validator = ConfigValidator.builder()
                    .requireSection("settings", nestedValidator)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).contains("enabled").contains("missing");
        }
    }

    // =========================================================================
    // List Tests
    // =========================================================================

    @Nested
    @DisplayName("requireList()")
    class ListTests {

        @Test
        @DisplayName("passes when list has required elements")
        void passesWithRequiredElements() {
            when(mockConfig.contains("tags")).thenReturn(true);
            when(mockConfig.getStringList("tags")).thenReturn(List.of("tag1", "tag2"));

            ConfigValidator validator = ConfigValidator.builder()
                    .requireList("tags", 1)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("fails when list has too few elements")
        void failsWithTooFewElements() {
            when(mockConfig.contains("tags")).thenReturn(true);
            when(mockConfig.getStringList("tags")).thenReturn(List.of());

            ConfigValidator validator = ConfigValidator.builder()
                    .requireList("tags", 1)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).contains("tags").contains("at least");
        }

        @Test
        @DisplayName("fails when list has too many elements")
        void failsWithTooManyElements() {
            when(mockConfig.contains("tags")).thenReturn(true);
            when(mockConfig.getStringList("tags")).thenReturn(List.of("a", "b", "c", "d"));

            ConfigValidator validator = ConfigValidator.builder()
                    .requireList("tags", 1, 3)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).contains("tags").contains("at most");
        }
    }

    // =========================================================================
    // Multiple Rules Tests
    // =========================================================================

    @Nested
    @DisplayName("Multiple Rules")
    class MultipleRulesTests {

        @Test
        @DisplayName("collects all errors from multiple rules")
        void collectsAllErrors() {
            when(mockConfig.contains("name")).thenReturn(false);
            when(mockConfig.contains("level")).thenReturn(false);
            when(mockConfig.contains("status")).thenReturn(false);

            ConfigValidator validator = ConfigValidator.builder()
                    .require("name", String.class)
                    .require("level", Integer.class)
                    .require("status", String.class)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("passes when all rules pass")
        void passesWhenAllPass() {
            when(mockConfig.contains("name")).thenReturn(true);
            when(mockConfig.get("name", String.class)).thenReturn("Test");
            when(mockConfig.contains("level")).thenReturn(true);
            when(mockConfig.getDouble("level")).thenReturn(50.0);

            ConfigValidator validator = ConfigValidator.builder()
                    .require("name", String.class)
                    .requireRange("level", 1, 100)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.isValid()).isTrue();
        }
    }

    // =========================================================================
    // Result Integration Tests
    // =========================================================================

    @Nested
    @DisplayName("validateAsResult()")
    class ResultTests {

        @Test
        @DisplayName("returns Success when valid")
        void returnsSuccessWhenValid() {
            when(mockConfig.contains("name")).thenReturn(true);
            when(mockConfig.get("name", String.class)).thenReturn("Test");

            ConfigValidator validator = ConfigValidator.builder()
                    .require("name", String.class)
                    .build();

            Result<ConfigSection> result = validator.validateAsResult(mockConfig);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).contains(mockConfig);
        }

        @Test
        @DisplayName("returns Failure when invalid")
        void returnsFailureWhenInvalid() {
            when(mockConfig.contains("name")).thenReturn(false);

            ConfigValidator validator = ConfigValidator.builder()
                    .require("name", String.class)
                    .build();

            Result<ConfigSection> result = validator.validateAsResult(mockConfig);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get()).contains("name");
        }
    }

    // =========================================================================
    // ValidationResult Tests
    // =========================================================================

    @Nested
    @DisplayName("ValidationResult")
    class ValidationResultTests {

        @Test
        @DisplayName("formatErrors produces readable output")
        void formatErrorsProducesReadableOutput() {
            when(mockConfig.contains("name")).thenReturn(false);
            when(mockConfig.contains("level")).thenReturn(false);

            ConfigValidator validator = ConfigValidator.builder()
                    .require("name", String.class)
                    .require("level", Integer.class)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);
            String formatted = result.formatErrors();

            assertThat(formatted).contains("Validation failed");
            assertThat(formatted).contains("2 error");
            assertThat(formatted).contains("1.");
            assertThat(formatted).contains("2.");
        }

        @Test
        @DisplayName("formatErrors handles empty errors")
        void formatErrorsHandlesEmpty() {
            when(mockConfig.contains("name")).thenReturn(true);
            when(mockConfig.get("name", String.class)).thenReturn("Test");

            ConfigValidator validator = ConfigValidator.builder()
                    .require("name", String.class)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);
            String formatted = result.formatErrors();

            assertThat(formatted).isEqualTo("No errors");
        }
    }

    // =========================================================================
    // Builder Tests
    // =========================================================================

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("ruleCount returns correct count")
        void ruleCountReturnsCorrectCount() {
            ConfigValidator validator = ConfigValidator.builder()
                    .require("a", String.class)
                    .require("b", Integer.class)
                    .optional("c", Boolean.class)
                    .build();

            assertThat(validator.ruleCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("builder with context path prefixes errors")
        void builderWithContextPath() {
            when(mockConfig.contains("name")).thenReturn(false);

            ConfigValidator validator = ConfigValidator.builder("quest.rewards")
                    .require("name", String.class)
                    .build();

            ConfigValidator.ValidationResult result = validator.validate(mockConfig);

            assertThat(result.errors().get(0)).contains("quest.rewards.name");
        }
    }
}
