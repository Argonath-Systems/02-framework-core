package com.argonathsystems.framework.core.validation;

import com.argonathsystems.framework.core.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Validators")
class ValidatorsTest {

    @Nested
    @DisplayName("requireNonNull")
    class RequireNonNullTests {

        @Test
        @DisplayName("returns value when non-null")
        void returnsValue() {
            String result = Validators.requireNonNull("value", "test");
            assertThat(result).isEqualTo("value");
        }

        @Test
        @DisplayName("throws when null")
        void throwsWhenNull() {
            assertThatThrownBy(() -> Validators.requireNonNull(null, "test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("test")
                    .hasMessageContaining("null");
        }
    }

    @Nested
    @DisplayName("requireNonEmpty for strings")
    class RequireNonEmptyStringTests {

        @Test
        @DisplayName("returns value when non-empty")
        void returnsValue() {
            String result = Validators.requireNonEmpty("value", "test");
            assertThat(result).isEqualTo("value");
        }

        @Test
        @DisplayName("throws when empty")
        void throwsWhenEmpty() {
            assertThatThrownBy(() -> Validators.requireNonEmpty("", "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws when null")
        void throwsWhenNull() {
            assertThatThrownBy(() -> Validators.requireNonEmpty((String) null, "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("requireNonBlank")
    class RequireNonBlankTests {

        @Test
        @DisplayName("returns trimmed value")
        void returnsTrimmedValue() {
            String result = Validators.requireNonBlank("  value  ", "test");
            assertThat(result).isEqualTo("value");
        }

        @Test
        @DisplayName("throws when blank")
        void throwsWhenBlank() {
            assertThatThrownBy(() -> Validators.requireNonBlank("   ", "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("requirePositive for int")
    class RequirePositiveIntTests {

        @Test
        @DisplayName("returns positive value")
        void returnsPositive() {
            int result = Validators.requirePositive(5, "test");
            assertThat(result).isEqualTo(5);
        }

        @Test
        @DisplayName("throws when zero")
        void throwsWhenZero() {
            assertThatThrownBy(() -> Validators.requirePositive(0, "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws when negative")
        void throwsWhenNegative() {
            assertThatThrownBy(() -> Validators.requirePositive(-1, "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("requireInRange")
    class RequireInRangeTests {

        @Test
        @DisplayName("returns value in range")
        void returnsValueInRange() {
            int result = Validators.requireInRange(5, 1, 10, "test");
            assertThat(result).isEqualTo(5);
        }

        @Test
        @DisplayName("accepts boundary values")
        void acceptsBoundaries() {
            assertThat(Validators.requireInRange(1, 1, 10, "test")).isEqualTo(1);
            assertThat(Validators.requireInRange(10, 1, 10, "test")).isEqualTo(10);
        }

        @Test
        @DisplayName("throws when below range")
        void throwsWhenBelow() {
            assertThatThrownBy(() -> Validators.requireInRange(0, 1, 10, "test"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between");
        }

        @Test
        @DisplayName("throws when above range")
        void throwsWhenAbove() {
            assertThatThrownBy(() -> Validators.requireInRange(11, 1, 10, "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("validatePositive")
    class ValidatePositiveTests {

        @Test
        @DisplayName("returns success for positive")
        void successForPositive() {
            Result<Integer> result = Validators.validatePositive(5, "test");
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).contains(5);
        }

        @Test
        @DisplayName("returns failure for zero")
        void failureForZero() {
            Result<Integer> result = Validators.validatePositive(0, "test");
            assertThat(result.isFailure()).isTrue();
        }
    }

    @Nested
    @DisplayName("requireNonEmpty for collections")
    class RequireNonEmptyCollectionTests {

        @Test
        @DisplayName("returns collection when non-empty")
        void returnsCollection() {
            List<String> list = List.of("a", "b");
            List<String> result = Validators.requireNonEmpty(list, "test");
            assertThat(result).containsExactly("a", "b");
        }

        @Test
        @DisplayName("throws when empty")
        void throwsWhenEmpty() {
            assertThatThrownBy(() -> Validators.requireNonEmpty(List.of(), "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
