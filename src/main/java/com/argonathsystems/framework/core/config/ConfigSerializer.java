package com.argonathsystems.framework.core.config;

import com.argonathsystems.framework.accessorapi.data.DataValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * @deprecated Use {@link #serializeTyped(Object)} for type-safe serialization.
     *             This method is retained for YAML/JSON library compatibility.
     */
    @Deprecated(since = "2.0.0")
    java.util.Map<String, Object> serialize(T object);

    /**
     * Serialize to a type-safe DataValue map.
     * Provides compile-time type safety for serialization.
     * 
     * <p>Default implementation converts from {@link #serialize(Object)}.
     * Override for direct type-safe implementation.
     *
     * @param object The object to serialize
     * @return Type-safe map representation
     * @since 2.0.0
     */
    default Map<String, DataValue> serializeTyped(T object) {
        Map<String, Object> raw = serialize(object);
        return DataValueConverter.fromRawMap(raw);
    }

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
     * @return The type identifier string, or empty if not a polymorphic type
     */
    default Optional<String> getTypeId() {
        return Optional.empty();
    }
}
