package com.argonathsystems.framework.core.config;

import com.argonathsystems.framework.core.result.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates configuration sections against defined schemas.
 * Provides fluent API for building validation rules.
 *
 * <p>Example usage:
 * <pre>{@code
 * ConfigValidator validator = ConfigValidator.builder()
 *     .require("name", String.class)
 *     .require("level", Integer.class, v -> v >= 1 && v <= 100)
 *     .optional("description", String.class)
 *     .requireSection("settings")
 *     .build();
 *
 * ValidationResult result = validator.validate(config);
 * if (!result.isValid()) {
 *     result.errors().forEach(System.err::println);
 * }
 * }</pre>
 *
 * @author Argonath Systems Team
 * @version 2.1.0
 * @since 2.1.0
 */
public final class ConfigValidator {

    private final List<ValidationRule> rules;
    private final String contextPath;

    private ConfigValidator(List<ValidationRule> rules, String contextPath) {
        this.rules = List.copyOf(rules);
        this.contextPath = contextPath;
    }

    /**
     * Create a new validator builder.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder("");
    }

    /**
     * Create a new validator builder with a context path prefix.
     *
     * @param contextPath Path prefix for error messages (e.g., "quest.rewards")
     * @return A new builder instance
     */
    public static Builder builder(String contextPath) {
        return new Builder(contextPath);
    }

    /**
     * Validate a config section against the defined rules.
     *
     * @param config The config section to validate
     * @return Validation result with any errors
     */
    public ValidationResult validate(ConfigSection config) {
        List<String> errors = new ArrayList<>();

        for (ValidationRule rule : rules) {
            List<String> ruleErrors = rule.validate(config, contextPath);
            errors.addAll(ruleErrors);
        }

        return new ValidationResult(errors);
    }

    /**
     * Validate and return a Result type for functional composition.
     *
     * @param config The config section to validate
     * @return Success with config if valid, Failure with concatenated errors if not
     */
    public Result<ConfigSection> validateAsResult(ConfigSection config) {
        ValidationResult result = validate(config);
        if (result.isValid()) {
            return Result.success(config);
        }
        return Result.failure(String.join("; ", result.errors()));
    }

    /**
     * Get the number of validation rules.
     *
     * @return Number of rules
     */
    public int ruleCount() {
        return rules.size();
    }

    // =========================================================================
    // Validation Result
    // =========================================================================

    /**
     * Result of a validation operation.
     *
     * @param errors List of validation error messages (empty if valid)
     */
    public record ValidationResult(List<String> errors) {

        /**
         * Creates a validation result.
         *
         * @param errors The list of errors
         */
        public ValidationResult {
            errors = List.copyOf(errors);
        }

        /**
         * Check if validation passed.
         *
         * @return true if no errors
         */
        public boolean isValid() {
            return errors.isEmpty();
        }

        /**
         * Check if validation failed.
         *
         * @return true if there are errors
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        /**
         * Get error count.
         *
         * @return Number of errors
         */
        public int errorCount() {
            return errors.size();
        }

        /**
         * Format errors as a multi-line string.
         *
         * @return Formatted error string
         */
        public String formatErrors() {
            if (errors.isEmpty()) {
                return "No errors";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Validation failed with ").append(errors.size()).append(" error(s):\n");
            for (int i = 0; i < errors.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
            }
            return sb.toString();
        }
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Builder for constructing ConfigValidator instances.
     */
    public static final class Builder {

        private final List<ValidationRule> rules = new ArrayList<>();
        private final String contextPath;

        private Builder(String contextPath) {
            this.contextPath = contextPath;
        }

        /**
         * Require a field to be present with the expected type.
         *
         * @param path Path to the field
         * @param type Expected type class
         * @param <T>  Field type
         * @return This builder
         */
        public <T> Builder require(String path, Class<T> type) {
            rules.add(new RequiredFieldRule<>(path, type, null));
            return this;
        }

        /**
         * Require a field with additional validation.
         *
         * @param path      Path to the field
         * @param type      Expected type class
         * @param validator Custom validation predicate
         * @param <T>       Field type
         * @return This builder
         */
        public <T> Builder require(String path, Class<T> type, FieldValidator<T> validator) {
            rules.add(new RequiredFieldRule<>(path, type, validator));
            return this;
        }

        /**
         * Mark a field as optional (validates type if present).
         *
         * @param path Path to the field
         * @param type Expected type class
         * @param <T>  Field type
         * @return This builder
         */
        public <T> Builder optional(String path, Class<T> type) {
            rules.add(new OptionalFieldRule<>(path, type, null));
            return this;
        }

        /**
         * Mark an optional field with validation if present.
         *
         * @param path      Path to the field
         * @param type      Expected type class
         * @param validator Custom validation predicate
         * @param <T>       Field type
         * @return This builder
         */
        public <T> Builder optional(String path, Class<T> type, FieldValidator<T> validator) {
            rules.add(new OptionalFieldRule<>(path, type, validator));
            return this;
        }

        /**
         * Require a string field to match a regex pattern.
         *
         * @param path    Path to the field
         * @param pattern Regex pattern
         * @return This builder
         */
        public Builder requirePattern(String path, String pattern) {
            return requirePattern(path, Pattern.compile(pattern));
        }

        /**
         * Require a string field to match a regex pattern.
         *
         * @param path    Path to the field
         * @param pattern Compiled regex pattern
         * @return This builder
         */
        public Builder requirePattern(String path, Pattern pattern) {
            rules.add(new PatternRule(path, pattern, true));
            return this;
        }

        /**
         * Require a numeric field to be within a range.
         *
         * @param path Path to the field
         * @param min  Minimum value (inclusive)
         * @param max  Maximum value (inclusive)
         * @return This builder
         */
        public Builder requireRange(String path, Number min, Number max) {
            rules.add(new RangeRule(path, min, max, true));
            return this;
        }

        /**
         * Optional range validation (validates only if field present).
         *
         * @param path Path to the field
         * @param min  Minimum value (inclusive)
         * @param max  Maximum value (inclusive)
         * @return This builder
         */
        public Builder optionalRange(String path, Number min, Number max) {
            rules.add(new RangeRule(path, min, max, false));
            return this;
        }

        /**
         * Require a string field to be one of allowed values.
         *
         * @param path          Path to the field
         * @param allowedValues Set of allowed values
         * @return This builder
         */
        public Builder requireOneOf(String path, Set<String> allowedValues) {
            rules.add(new OneOfRule(path, allowedValues, true));
            return this;
        }

        /**
         * Require a string field to be one of allowed enum values.
         *
         * @param path     Path to the field
         * @param enumType Enum class
         * @param <E>      Enum type
         * @return This builder
         */
        public <E extends Enum<E>> Builder requireEnum(String path, Class<E> enumType) {
            Set<String> values = new java.util.HashSet<>();
            for (E constant : enumType.getEnumConstants()) {
                values.add(constant.name());
            }
            return requireOneOf(path, values);
        }

        /**
         * Require a nested section to exist.
         *
         * @param path Path to the section
         * @return This builder
         */
        public Builder requireSection(String path) {
            rules.add(new SectionRule(path, null, true));
            return this;
        }

        /**
         * Require a nested section with its own validator.
         *
         * @param path           Path to the section
         * @param nestedValidator Validator for the nested section
         * @return This builder
         */
        public Builder requireSection(String path, ConfigValidator nestedValidator) {
            rules.add(new SectionRule(path, nestedValidator, true));
            return this;
        }

        /**
         * Optional section with validator if present.
         *
         * @param path           Path to the section
         * @param nestedValidator Validator for the nested section
         * @return This builder
         */
        public Builder optionalSection(String path, ConfigValidator nestedValidator) {
            rules.add(new SectionRule(path, nestedValidator, false));
            return this;
        }

        /**
         * Require a list of strings.
         *
         * @param path Path to the list
         * @return This builder
         */
        public Builder requireStringList(String path) {
            rules.add(new ListRule(path, true, 1, Integer.MAX_VALUE));
            return this;
        }

        /**
         * Require a list with minimum size.
         *
         * @param path    Path to the list
         * @param minSize Minimum number of elements
         * @return This builder
         */
        public Builder requireList(String path, int minSize) {
            rules.add(new ListRule(path, true, minSize, Integer.MAX_VALUE));
            return this;
        }

        /**
         * Require a list with size constraints.
         *
         * @param path    Path to the list
         * @param minSize Minimum number of elements
         * @param maxSize Maximum number of elements
         * @return This builder
         */
        public Builder requireList(String path, int minSize, int maxSize) {
            rules.add(new ListRule(path, true, minSize, maxSize));
            return this;
        }

        /**
         * Add a custom validation rule.
         *
         * @param rule The custom rule
         * @return This builder
         */
        public Builder addRule(ValidationRule rule) {
            rules.add(rule);
            return this;
        }

        /**
         * Build the validator.
         *
         * @return Configured validator instance
         */
        public ConfigValidator build() {
            return new ConfigValidator(rules, contextPath);
        }
    }

    // =========================================================================
    // Functional Interfaces
    // =========================================================================

    /**
     * Custom field validator.
     *
     * @param <T> Field type
     */
    @FunctionalInterface
    public interface FieldValidator<T> {

        /**
         * Validate a field value.
         *
         * @param value The value to validate
         * @return null if valid, error message if invalid
         */
        String validate(T value);
    }

    // =========================================================================
    // Validation Rules
    // =========================================================================

    /**
     * Base interface for validation rules.
     */
    public interface ValidationRule {

        /**
         * Validate against a config section.
         *
         * @param config      The config to validate
         * @param contextPath Current context path for error messages
         * @return List of error messages (empty if valid)
         */
        List<String> validate(ConfigSection config, String contextPath);
    }

    /**
     * Rule for required fields.
     */
    private record RequiredFieldRule<T>(
            String path,
            Class<T> type,
            FieldValidator<T> validator
    ) implements ValidationRule {

        @Override
        public List<String> validate(ConfigSection config, String contextPath) {
            List<String> errors = new ArrayList<>();
            String fullPath = contextPath.isEmpty() ? path : contextPath + "." + path;

            if (!config.contains(path)) {
                errors.add("Required field '" + fullPath + "' is missing");
                return errors;
            }

            try {
                T value = config.get(path, type);
                if (value == null) {
                    errors.add("Field '" + fullPath + "' is null");
                    return errors;
                }

                if (validator != null) {
                    String error = validator.validate(value);
                    if (error != null) {
                        errors.add("Field '" + fullPath + "': " + error);
                    }
                }
            } catch (ClassCastException e) {
                errors.add("Field '" + fullPath + "' has wrong type. Expected: " + type.getSimpleName());
            } catch (Exception e) {
                errors.add("Field '" + fullPath + "' could not be read: " + e.getMessage());
            }

            return errors;
        }
    }

    /**
     * Rule for optional fields (validates type if present).
     */
    private record OptionalFieldRule<T>(
            String path,
            Class<T> type,
            FieldValidator<T> validator
    ) implements ValidationRule {

        @Override
        public List<String> validate(ConfigSection config, String contextPath) {
            if (!config.contains(path)) {
                return List.of(); // Optional - OK if missing
            }

            List<String> errors = new ArrayList<>();
            String fullPath = contextPath.isEmpty() ? path : contextPath + "." + path;

            try {
                T value = config.get(path, type);
                if (value != null && validator != null) {
                    String error = validator.validate(value);
                    if (error != null) {
                        errors.add("Field '" + fullPath + "': " + error);
                    }
                }
            } catch (ClassCastException e) {
                errors.add("Field '" + fullPath + "' has wrong type. Expected: " + type.getSimpleName());
            } catch (Exception e) {
                errors.add("Field '" + fullPath + "' could not be read: " + e.getMessage());
            }

            return errors;
        }
    }

    /**
     * Rule for regex pattern matching.
     */
    private record PatternRule(
            String path,
            Pattern pattern,
            boolean required
    ) implements ValidationRule {

        @Override
        public List<String> validate(ConfigSection config, String contextPath) {
            List<String> errors = new ArrayList<>();
            String fullPath = contextPath.isEmpty() ? path : contextPath + "." + path;

            if (!config.contains(path)) {
                if (required) {
                    errors.add("Required field '" + fullPath + "' is missing");
                }
                return errors;
            }

            String value = config.getString(path);
            if (value == null) {
                if (required) {
                    errors.add("Field '" + fullPath + "' is null");
                }
                return errors;
            }

            if (!pattern.matcher(value).matches()) {
                errors.add("Field '" + fullPath + "' does not match pattern: " + pattern.pattern());
            }

            return errors;
        }
    }

    /**
     * Rule for numeric range validation.
     */
    private record RangeRule(
            String path,
            Number min,
            Number max,
            boolean required
    ) implements ValidationRule {

        @Override
        public List<String> validate(ConfigSection config, String contextPath) {
            List<String> errors = new ArrayList<>();
            String fullPath = contextPath.isEmpty() ? path : contextPath + "." + path;

            if (!config.contains(path)) {
                if (required) {
                    errors.add("Required field '" + fullPath + "' is missing");
                }
                return errors;
            }

            double value = config.getDouble(path);
            if (value < min.doubleValue() || value > max.doubleValue()) {
                errors.add("Field '" + fullPath + "' must be between " + min + " and " + max + ", got: " + value);
            }

            return errors;
        }
    }

    /**
     * Rule for enum/set membership validation.
     */
    private record OneOfRule(
            String path,
            Set<String> allowedValues,
            boolean required
    ) implements ValidationRule {

        @Override
        public List<String> validate(ConfigSection config, String contextPath) {
            List<String> errors = new ArrayList<>();
            String fullPath = contextPath.isEmpty() ? path : contextPath + "." + path;

            if (!config.contains(path)) {
                if (required) {
                    errors.add("Required field '" + fullPath + "' is missing");
                }
                return errors;
            }

            String value = config.getString(path);
            if (value == null) {
                if (required) {
                    errors.add("Field '" + fullPath + "' is null");
                }
                return errors;
            }

            if (!allowedValues.contains(value)) {
                errors.add("Field '" + fullPath + "' must be one of " + allowedValues + ", got: " + value);
            }

            return errors;
        }
    }

    /**
     * Rule for nested section validation.
     */
    private record SectionRule(
            String path,
            ConfigValidator nestedValidator,
            boolean required
    ) implements ValidationRule {

        @Override
        public List<String> validate(ConfigSection config, String contextPath) {
            List<String> errors = new ArrayList<>();
            String fullPath = contextPath.isEmpty() ? path : contextPath + "." + path;

            ConfigSection section = config.getSection(path);
            if (section == null) {
                if (required) {
                    errors.add("Required section '" + fullPath + "' is missing");
                }
                return errors;
            }

            if (nestedValidator != null) {
                ValidationResult nestedResult = nestedValidator.validate(section);
                // Prepend the path to nested errors
                for (String error : nestedResult.errors()) {
                    errors.add(error);
                }
            }

            return errors;
        }
    }

    /**
     * Rule for list validation.
     */
    private record ListRule(
            String path,
            boolean required,
            int minSize,
            int maxSize
    ) implements ValidationRule {

        @Override
        public List<String> validate(ConfigSection config, String contextPath) {
            List<String> errors = new ArrayList<>();
            String fullPath = contextPath.isEmpty() ? path : contextPath + "." + path;

            if (!config.contains(path)) {
                if (required) {
                    errors.add("Required list '" + fullPath + "' is missing");
                }
                return errors;
            }

            List<String> list = config.getStringList(path);
            if (list == null) {
                if (required) {
                    errors.add("Field '" + fullPath + "' is not a list");
                }
                return errors;
            }

            if (list.size() < minSize) {
                errors.add("List '" + fullPath + "' must have at least " + minSize + " element(s), has: " + list.size());
            }

            if (list.size() > maxSize) {
                errors.add("List '" + fullPath + "' must have at most " + maxSize + " element(s), has: " + list.size());
            }

            return errors;
        }
    }
}
