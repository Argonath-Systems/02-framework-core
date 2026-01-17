package com.argonathsystems.framework.core.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for serializers by type ID.
 * Used for polymorphic deserialization of config objects.
 */
public class SerializerRegistry {

    private final Map<String, ConfigSerializer<?>> serializers = new HashMap<>();

    /**
     * Register a serializer for a type.
     *
     * @param typeId     The type identifier
     * @param serializer The serializer for that type
     * @param <T>        The type being serialized
     */
    public <T> void register(String typeId, ConfigSerializer<T> serializer) {
        serializers.put(typeId, serializer);
    }

    /**
     * Get a serializer by type ID.
     *
     * @param typeId The type identifier
     * @param <T>    The expected type
     * @return The serializer, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> ConfigSerializer<T> get(String typeId) {
        return (ConfigSerializer<T>) serializers.get(typeId);
    }

    /**
     * Check if a type is registered.
     *
     * @param typeId The type identifier
     * @return true if a serializer is registered
     */
    public boolean hasSerializer(String typeId) {
        return serializers.containsKey(typeId);
    }

    /**
     * Deserialize an object using its 'type' field to determine the serializer.
     *
     * @param config The config section containing the object
     * @param <T>    The expected return type
     * @return The deserialized object
     * @throws ConfigException if the type field is missing or unknown
     */
    public <T> T deserialize(ConfigSection config) {
        String type = config.getString("type");
        if (type == null) {
            throw new ConfigException("Missing 'type' field in config");
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

    /**
     * Get all registered type IDs.
     *
     * @return Set of registered type identifiers
     */
    public java.util.Set<String> getRegisteredTypes() {
        return serializers.keySet();
    }
}
