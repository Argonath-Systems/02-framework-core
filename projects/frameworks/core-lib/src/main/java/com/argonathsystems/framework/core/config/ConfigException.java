package com.argonathsystems.framework.core.config;

/**
 * Exception thrown when configuration loading or parsing fails.
 */
public class ConfigException extends RuntimeException {

    /**
     * Create a config exception with a message.
     *
     * @param message The error message
     */
    public ConfigException(String message) {
        super(message);
    }

    /**
     * Create a config exception with a message and cause.
     *
     * @param message The error message
     * @param cause   The underlying cause
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
