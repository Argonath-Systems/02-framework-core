package com.argonathsystems.framework.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SerializerRegistryTest {

    private SerializerRegistry registry;
    private ConfigSerializer<String> mockSerializer;

    @BeforeEach
    void setUp() {
        registry = new SerializerRegistry();
        mockSerializer = spy(new ConfigSerializer<>() {
            @Override
            public String deserialize(ConfigSection config) {
                return "deserialized_value";
            }

            @Override
            public Map<String, Object> serialize(String object) {
                return Map.of("val", object);
            }
        });
    }

    @Test
    void testRegisterAndGet() {
        registry.register("test_type", mockSerializer);
        
        assertTrue(registry.hasSerializer("test_type"));
        assertSame(mockSerializer, registry.get("test_type"));
        assertNull(registry.get("unknown_type"));
    }

    @Test
    void testDeserializeSuccess() {
        registry.register("test_type", mockSerializer);

        ConfigSection config = mock(ConfigSection.class);
        when(config.getString("type")).thenReturn("test_type");

        String result = registry.deserialize(config);
        
        assertEquals("deserialized_value", result);
        verify(mockSerializer).deserialize(config);
        verify(mockSerializer).validate(config);
    }

    @Test
    void testDeserializeMissingType() {
        ConfigSection config = mock(ConfigSection.class);
        when(config.getString("type")).thenReturn(null);

        ConfigException ex = assertThrows(ConfigException.class, () -> 
            registry.deserialize(config)
        );
        assertEquals("Missing 'type' field in config", ex.getMessage());
    }

    @Test
    void testDeserializeUnknownType() {
        ConfigSection config = mock(ConfigSection.class);
        when(config.getString("type")).thenReturn("unknown_type");

        ConfigException ex = assertThrows(ConfigException.class, () -> 
            registry.deserialize(config)
        );
        assertEquals("Unknown type: unknown_type", ex.getMessage());
    }

    @Test
    void testDeserializeValidationFailure() {
        registry.register("test_type", mockSerializer);
        
        // Make validator fail
        doReturn(List.of("validation error")).when(mockSerializer).validate(any());

        ConfigSection config = mock(ConfigSection.class);
        when(config.getString("type")).thenReturn("test_type");

        ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> 
            registry.deserialize(config)
        );
        
        assertEquals("test_type", ex.getTypeId()); // Assuming getter exists or check message
        assertTrue(ex.getMessage().contains("validation error") || ex.getErrors().contains("validation error"));
    }
}
