package com.argonathsystems.framework.core.config;

import java.util.List;

/**
 * Exception thrown when configuration validation fails.
 */
public class ConfigValidationException extends ConfigException {

    private final String typeId;
    private final List<String> errors;

    /**
     * Create a validation exception.
     *
     * @param typeId The type that failed validation
     * @param errors List of validation error messages
     */
    public ConfigValidationException(String typeId, List<String> errors) {
        super(formatMessage(typeId, errors));
        this.typeId = typeId;
        this.errors = List.copyOf(errors);
    }

    private static String formatMessage(String typeId, List<String> errors) {
        StringBuilder sb = new StringBuilder();
        sb.append("Validation failed for type '").append(typeId).append("':");
        for (String error : errors) {
            sb.append("\n  - ").append(error);
        }
        return sb.toString();
    }

    /**
     * Get the type ID that failed validation.
     *
     * @return The type identifier
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * Get the list of validation errors.
     *
     * @return Immutable list of error messages
     */
    public List<String> getErrors() {
        return errors;
    }
}
