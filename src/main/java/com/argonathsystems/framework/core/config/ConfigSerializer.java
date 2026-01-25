package com.argonathsystems.framework.core.config;

import java.util.List;

/**
 * Serializer for converting objects to/from config.
 *
 * @param <T> The type this serializer handles
 */
public interface ConfigSerializer<T> {

    /**
     * Deserialize from config section.
     *
     * @param config The config section to read from
     * @return The deserialized object
     * @throws ConfigException if deserialization fails
     */
    T deserialize(ConfigSection config);

    /**
     * Serialize to a map representation.
     * The returned map can be written to YAML/JSON.
     *
     * @param object The object to serialize
     * @return Map representation suitable for config output
     */
    java.util.Map<String, Object> serialize(T object);

    /**
     * Validate config before deserialization.
     *
     * @param config The config to validate
     * @return List of validation error messages (empty if valid)
     */
    default List<String> validate(ConfigSection config) {
        return List.of();
    }

    /**
     * Get the type identifier for this serializer.
     * Used for polymorphic deserialization.
     *
     * @return The type identifier string
     */
    default String getTypeId() {
        return null;
    }
}
