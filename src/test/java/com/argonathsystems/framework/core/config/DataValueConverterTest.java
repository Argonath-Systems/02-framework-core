package com.argonathsystems.framework.core.config;

import com.argonathsystems.framework.accessorapi.data.DataValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DataValueConverter utility class.
 */
@DisplayName("DataValueConverter")
class DataValueConverterTest {

    // =========================================================================
    // fromRawValue Tests
    // =========================================================================

    @Nested
    @DisplayName("fromRawValue()")
    class FromRawValueTests {

        @Test
        @DisplayName("converts String to StringValue")
        void convertsString() {
            DataValue result = DataValueConverter.fromRawValue("hello");

            assertThat(result).isInstanceOf(DataValue.StringValue.class);
            assertThat(((DataValue.StringValue) result).value()).isEqualTo("hello");
        }

        @Test
        @DisplayName("converts Integer to IntValue")
        void convertsInteger() {
            DataValue result = DataValueConverter.fromRawValue(42);

            assertThat(result).isInstanceOf(DataValue.IntValue.class);
            assertThat(((DataValue.IntValue) result).value()).isEqualTo(42);
        }

        @Test
        @DisplayName("converts Long to LongValue")
        void convertsLong() {
            DataValue result = DataValueConverter.fromRawValue(9999999999L);

            assertThat(result).isInstanceOf(DataValue.LongValue.class);
            assertThat(((DataValue.LongValue) result).value()).isEqualTo(9999999999L);
        }

        @Test
        @DisplayName("converts Double to DoubleValue")
        void convertsDouble() {
            DataValue result = DataValueConverter.fromRawValue(3.14159);

            assertThat(result).isInstanceOf(DataValue.DoubleValue.class);
            assertThat(((DataValue.DoubleValue) result).value()).isCloseTo(3.14159, within(0.00001));
        }

        @Test
        @DisplayName("converts Float to DoubleValue")
        void convertsFloat() {
            DataValue result = DataValueConverter.fromRawValue(2.5f);

            assertThat(result).isInstanceOf(DataValue.DoubleValue.class);
            assertThat(((DataValue.DoubleValue) result).value()).isCloseTo(2.5, within(0.00001));
        }

        @Test
        @DisplayName("converts Boolean to BoolValue")
        void convertsBoolean() {
            DataValue trueResult = DataValueConverter.fromRawValue(true);
            DataValue falseResult = DataValueConverter.fromRawValue(false);

            assertThat(trueResult).isInstanceOf(DataValue.BoolValue.class);
            assertThat(((DataValue.BoolValue) trueResult).value()).isTrue();

            assertThat(falseResult).isInstanceOf(DataValue.BoolValue.class);
            assertThat(((DataValue.BoolValue) falseResult).value()).isFalse();
        }

        @Test
        @DisplayName("converts List to ListValue")
        void convertsList() {
            List<Object> raw = List.of("a", "b", "c");
            DataValue result = DataValueConverter.fromRawValue(raw);

            assertThat(result).isInstanceOf(DataValue.ListValue.class);
            DataValue.ListValue listValue = (DataValue.ListValue) result;
            assertThat(listValue.value()).hasSize(3);
            assertThat(listValue.value().get(0)).isInstanceOf(DataValue.StringValue.class);
        }

        @Test
        @DisplayName("converts Map to MapValue")
        void convertsMap() {
            Map<String, Object> raw = Map.of("key", "value", "count", 10);
            DataValue result = DataValueConverter.fromRawValue(raw);

            assertThat(result).isInstanceOf(DataValue.MapValue.class);
            DataValue.MapValue mapValue = (DataValue.MapValue) result;
            assertThat(mapValue.value()).containsKey("key");
            assertThat(mapValue.value()).containsKey("count");
        }

        @Test
        @DisplayName("converts null to empty StringValue")
        void convertsNull() {
            DataValue result = DataValueConverter.fromRawValue(null);

            assertThat(result).isInstanceOf(DataValue.StringValue.class);
            assertThat(((DataValue.StringValue) result).value()).isEmpty();
        }

        @Test
        @DisplayName("converts unknown type to StringValue via toString()")
        void convertsUnknownType() {
            Object customObject = new Object() {
                @Override
                public String toString() {
                    return "custom-string";
                }
            };
            DataValue result = DataValueConverter.fromRawValue(customObject);

            assertThat(result).isInstanceOf(DataValue.StringValue.class);
            assertThat(((DataValue.StringValue) result).value()).isEqualTo("custom-string");
        }

        @Test
        @DisplayName("handles nested structures")
        void handlesNestedStructures() {
            Map<String, Object> nested = Map.of(
                    "name", "test",
                    "items", List.of(1, 2, 3),
                    "config", Map.of("enabled", true)
            );

            DataValue result = DataValueConverter.fromRawValue(nested);

            assertThat(result).isInstanceOf(DataValue.MapValue.class);
            DataValue.MapValue mapValue = (DataValue.MapValue) result;

            assertThat(mapValue.value().get("name")).isInstanceOf(DataValue.StringValue.class);
            assertThat(mapValue.value().get("items")).isInstanceOf(DataValue.ListValue.class);
            assertThat(mapValue.value().get("config")).isInstanceOf(DataValue.MapValue.class);
        }
    }

    // =========================================================================
    // toRawValue Tests
    // =========================================================================

    @Nested
    @DisplayName("toRawValue()")
    class ToRawValueTests {

        @Test
        @DisplayName("converts StringValue to String")
        void convertsStringValue() {
            Object result = DataValueConverter.toRawValue(DataValue.of("hello"));

            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("converts IntValue to Integer")
        void convertsIntValue() {
            Object result = DataValueConverter.toRawValue(DataValue.of(42));

            assertThat(result).isEqualTo(42);
        }

        @Test
        @DisplayName("converts LongValue to Long")
        void convertsLongValue() {
            Object result = DataValueConverter.toRawValue(DataValue.of(9999999999L));

            assertThat(result).isEqualTo(9999999999L);
        }

        @Test
        @DisplayName("converts DoubleValue to Double")
        void convertsDoubleValue() {
            Object result = DataValueConverter.toRawValue(DataValue.of(3.14));

            assertThat(result).isEqualTo(3.14);
        }

        @Test
        @DisplayName("converts BoolValue to Boolean")
        void convertsBoolValue() {
            Object trueResult = DataValueConverter.toRawValue(DataValue.of(true));
            Object falseResult = DataValueConverter.toRawValue(DataValue.of(false));

            assertThat(trueResult).isEqualTo(true);
            assertThat(falseResult).isEqualTo(false);
        }

        @Test
        @DisplayName("converts ListValue to List")
        void convertsListValue() {
            List<DataValue> list = List.of(
                    DataValue.of("a"),
                    DataValue.of("b"),
                    DataValue.of("c")
            );
            Object result = DataValueConverter.toRawValue(DataValue.of(list));

            assertThat(result).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            List<Object> resultList = (List<Object>) result;
            assertThat(resultList).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("converts MapValue to Map")
        void convertsMapValue() {
            Map<String, DataValue> map = Map.of(
                    "name", DataValue.of("test"),
                    "count", DataValue.of(5)
            );
            Object result = DataValueConverter.toRawValue(DataValue.of(map));

            assertThat(result).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            assertThat(resultMap).containsEntry("name", "test");
            assertThat(resultMap).containsEntry("count", 5);
        }

        @Test
        @DisplayName("throws on null input")
        void throwsOnNull() {
            assertThatThrownBy(() -> DataValueConverter.toRawValue(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }
    }

    // =========================================================================
    // fromRawMap / toRawMap Tests
    // =========================================================================

    @Nested
    @DisplayName("Map Conversion")
    class MapConversionTests {

        @Test
        @DisplayName("fromRawMap converts entire map")
        void fromRawMapConvertsEntireMap() {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("string", "value");
            raw.put("number", 42);
            raw.put("flag", true);

            Map<String, DataValue> result = DataValueConverter.fromRawMap(raw);

            assertThat(result).hasSize(3);
            assertThat(result.get("string")).isInstanceOf(DataValue.StringValue.class);
            assertThat(result.get("number")).isInstanceOf(DataValue.IntValue.class);
            assertThat(result.get("flag")).isInstanceOf(DataValue.BoolValue.class);
        }

        @Test
        @DisplayName("fromRawMap handles null map")
        void fromRawMapHandlesNull() {
            Map<String, DataValue> result = DataValueConverter.fromRawMap(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("toRawMap converts entire map")
        void toRawMapConvertsEntireMap() {
            Map<String, DataValue> typed = new LinkedHashMap<>();
            typed.put("string", DataValue.of("value"));
            typed.put("number", DataValue.of(42));
            typed.put("flag", DataValue.of(true));

            Map<String, Object> result = DataValueConverter.toRawMap(typed);

            assertThat(result).hasSize(3);
            assertThat(result.get("string")).isEqualTo("value");
            assertThat(result.get("number")).isEqualTo(42);
            assertThat(result.get("flag")).isEqualTo(true);
        }

        @Test
        @DisplayName("toRawMap handles null map")
        void toRawMapHandlesNull() {
            Map<String, Object> result = DataValueConverter.toRawMap(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("round-trip conversion preserves data")
        void roundTripPreservesData() {
            Map<String, Object> original = new LinkedHashMap<>();
            original.put("name", "Quest");
            original.put("level", 10);
            original.put("enabled", true);
            original.put("ratio", 0.75);

            Map<String, DataValue> typed = DataValueConverter.fromRawMap(original);
            Map<String, Object> converted = DataValueConverter.toRawMap(typed);

            assertThat(converted).containsEntry("name", "Quest");
            assertThat(converted).containsEntry("level", 10);
            assertThat(converted).containsEntry("enabled", true);
            assertThat(converted).containsEntry("ratio", 0.75);
        }
    }
}
