package com.argonathsystems.framework.core.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Result")
class ResultTest {

    @Nested
    @DisplayName("Success")
    class SuccessTests {

        @Test
        @DisplayName("isSuccess returns true")
        void isSuccess() {
            Result<String> result = Result.success("value");
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isFailure()).isFalse();
        }

        @Test
        @DisplayName("getValue returns Optional with value")
        void getValue() {
            Result<String> result = Result.success("value");
            assertThat(result.getValue()).contains("value");
        }

        @Test
        @DisplayName("getError returns empty")
        void getError() {
            Result<String> result = Result.success("value");
            assertThat(result.getError()).isEmpty();
        }

        @Test
        @DisplayName("map transforms value")
        void map() {
            Result<Integer> result = Result.<String>success("hello")
                    .map(String::length);
            assertThat(result.getValue()).contains(5);
        }

        @Test
        @DisplayName("flatMap chains results")
        void flatMap() {
            Result<Integer> result = Result.<String>success("42")
                    .flatMap(s -> Result.success(Integer.parseInt(s)));
            assertThat(result.getValue()).contains(42);
        }

        @Test
        @DisplayName("orElse returns value")
        void orElse() {
            String value = Result.success("value").orElse("default");
            assertThat(value).isEqualTo("value");
        }

        @Test
        @DisplayName("orElseThrow returns value")
        void orElseThrow() {
            String value = Result.success("value").orElseThrow();
            assertThat(value).isEqualTo("value");
        }

        @Test
        @DisplayName("ifSuccess executes action")
        void ifSuccess() {
            AtomicReference<String> captured = new AtomicReference<>();
            Result.success("value").ifSuccess(captured::set);
            assertThat(captured.get()).isEqualTo("value");
        }

        @Test
        @DisplayName("ifFailure does not execute")
        void ifFailure() {
            AtomicBoolean called = new AtomicBoolean(false);
            Result.success("value").ifFailure(e -> called.set(true));
            assertThat(called.get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Failure")
    class FailureTests {

        @Test
        @DisplayName("isFailure returns true")
        void isFailure() {
            Result<String> result = Result.failure("error");
            assertThat(result.isFailure()).isTrue();
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("getValue returns empty")
        void getValue() {
            Result<String> result = Result.failure("error");
            assertThat(result.getValue()).isEmpty();
        }

        @Test
        @DisplayName("getError returns message")
        void getError() {
            Result<String> result = Result.failure("error message");
            assertThat(result.getError()).contains("error message");
        }

        @Test
        @DisplayName("failure with format")
        void failureWithFormat() {
            Result<String> result = Result.failure("Error: %s at %d", "test", 42);
            assertThat(result.getError()).contains("Error: test at 42");
        }

        @Test
        @DisplayName("map propagates failure")
        void map() {
            Result<Integer> result = Result.<String>failure("error")
                    .map(String::length);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("error");
        }

        @Test
        @DisplayName("flatMap propagates failure")
        void flatMap() {
            Result<Integer> result = Result.<String>failure("error")
                    .flatMap(s -> Result.success(s.length()));
            assertThat(result.isFailure()).isTrue();
        }

        @Test
        @DisplayName("orElse returns default")
        void orElse() {
            String value = Result.<String>failure("error").orElse("default");
            assertThat(value).isEqualTo("default");
        }

        @Test
        @DisplayName("orElseThrow throws exception")
        void orElseThrow() {
            Result<String> result = Result.failure("error message");
            assertThatThrownBy(result::orElseThrow)
                    .isInstanceOf(ResultException.class)
                    .hasMessage("error message");
        }

        @Test
        @DisplayName("ifSuccess does not execute")
        void ifSuccess() {
            AtomicBoolean called = new AtomicBoolean(false);
            Result.<String>failure("error").ifSuccess(v -> called.set(true));
            assertThat(called.get()).isFalse();
        }

        @Test
        @DisplayName("ifFailure executes action")
        void ifFailure() {
            AtomicReference<String> captured = new AtomicReference<>();
            Result.<String>failure("error").ifFailure(captured::set);
            assertThat(captured.get()).isEqualTo("error");
        }
    }

    @Nested
    @DisplayName("fromOptional")
    class FromOptionalTests {

        @Test
        @DisplayName("converts present to success")
        void convertsPresent() {
            Result<String> result = Result.fromOptional(
                    Optional.of("value"),
                    "Not found"
            );
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).contains("value");
        }

        @Test
        @DisplayName("converts empty to failure")
        void convertsEmpty() {
            Result<String> result = Result.fromOptional(
                    Optional.empty(),
                    "Not found"
            );
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Not found");
        }
    }
}
