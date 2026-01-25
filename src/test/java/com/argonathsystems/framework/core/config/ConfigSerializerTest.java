package com.argonathsystems.framework.core.config;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigSerializerTest {

    @Test
    void testDefaultMethods() {
        ConfigSerializer<String> serializer = new ConfigSerializer<>() {
            @Override
            public String deserialize(ConfigSection config) {
                return "test";
            }

            @Override
            public Map<String, Object> serialize(String object) {
                return Collections.emptyMap();
            }
        };

        // Test default validate
        ConfigSection mockConfig = mock(ConfigSection.class);
        List<String> errors = serializer.validate(mockConfig);
        assertNotNull(errors);
        assertTrue(errors.isEmpty(), "Default validate should return empty list");

        // Test default getTypeId
        assertNull(serializer.getTypeId(), "Default getTypeId should return null");
    }

    @Test
    void testCustomValidation() {
        ConfigSerializer<String> serializer = new ConfigSerializer<>() {
            @Override
            public String deserialize(ConfigSection config) {
                return "test";
            }

            @Override
            public Map<String, Object> serialize(String object) {
                return Collections.emptyMap();
            }

            @Override
            public List<String> validate(ConfigSection config) {
                return List.of("error");
            }
        };

        ConfigSection mockConfig = mock(ConfigSection.class);
        List<String> errors = serializer.validate(mockConfig);
        assertEquals(1, errors.size());
        assertEquals("error", errors.get(0));
    }
}
