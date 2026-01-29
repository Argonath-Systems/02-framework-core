package com.argonathsystems.framework.core.config;

import com.argonathsystems.framework.accessorapi.data.DataValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for converting between raw Object maps and type-safe DataValue maps.
 * Provides bidirectional conversion for YAML/JSON library compatibility.
 *
 * @author Argonath Systems Team
 * @version 2.0.0
 * @since 2.0.0
 */
public final class DataValueConverter {

    private DataValueConverter() {
        // Utility class - no instantiation
    }

    /**
     * Convert a raw Object map to a type-safe DataValue map.
     *
     * @param raw The raw map from YAML/JSON parsing
     * @return Type-safe DataValue map
     * @throws IllegalArgumentException if a value type is not supported
     */
    public static Map<String, DataValue> fromRawMap(Map<String, Object> raw) {
        if (raw == null) {
            return Map.of();
        }
        Map<String, DataValue> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(), fromRawValue(entry.getValue()));
        }
        return result;
    }

    /**
     * Convert a type-safe DataValue map to a raw Object map.
     * Useful for YAML/JSON serialization libraries.
     *
     * @param typed The type-safe map
     * @return Raw Object map suitable for YAML/JSON output
     */
    public static Map<String, Object> toRawMap(Map<String, DataValue> typed) {
        if (typed == null) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, DataValue> entry : typed.entrySet()) {
            result.put(entry.getKey(), toRawValue(entry.getValue()));
        }
        return result;
    }

    /**
     * Convert a single raw value to DataValue.
     *
     * @param value The raw value
     * @return Corresponding DataValue
     * @throws IllegalArgumentException if the value type is not supported
     */
    @SuppressWarnings("unchecked")
    public static DataValue fromRawValue(Object value) {
        if (value == null) {
            return DataValue.of("");  // Null treated as empty string
        }
        return switch (value) {
            case String s -> DataValue.of(s);
            case Integer i -> DataValue.of(i);
            case Long l -> DataValue.of(l);
            case Double d -> DataValue.of(d);
            case Float f -> DataValue.of(f.doubleValue());
            case Boolean b -> DataValue.of(b);
            case List<?> list -> DataValue.of(
                list.stream()
                    .map(DataValueConverter::fromRawValue)
                    .collect(Collectors.toList())
            );
            case Map<?, ?> map -> DataValue.of(
                fromRawMap((Map<String, Object>) map)
            );
            default -> DataValue.of(value.toString());
        };
    }

    /**
     * Convert a DataValue to a raw Object.
     *
     * @param value The DataValue (must not be null)
     * @return Raw Object suitable for YAML/JSON
     * @throws IllegalArgumentException if value is null
     */
    public static Object toRawValue(DataValue value) {
        if (value == null) {
            throw new IllegalArgumentException("DataValue cannot be null. Use DataValue.of(\"\") for empty values.");
        }
        return switch (value) {
            case DataValue.StringValue sv -> sv.value();
            case DataValue.IntValue iv -> iv.value();
            case DataValue.LongValue lv -> lv.value();
            case DataValue.DoubleValue dv -> dv.value();
            case DataValue.BoolValue bv -> bv.value();
            case DataValue.ListValue lv -> lv.value().stream()
                .map(DataValueConverter::toRawValue)
                .collect(Collectors.toList());
            case DataValue.MapValue mv -> toRawMap(mv.value());
        };
    }
}
