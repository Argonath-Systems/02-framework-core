package com.argonathsystems.framework.core.progress;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Progress")
class ProgressTest {

    @Nested
    @DisplayName("creation")
    class CreationTests {

        @Test
        @DisplayName("creates with required amount")
        void createsWithRequired() {
            Progress progress = Progress.of(10);
            assertThat(progress.current()).isEqualTo(0);
            assertThat(progress.required()).isEqualTo(10);
            assertThat(progress.completed()).isFalse();
        }

        @Test
        @DisplayName("creates with initial value")
        void createsWithInitialValue() {
            Progress progress = Progress.of(5, 10);
            assertThat(progress.current()).isEqualTo(5);
            assertThat(progress.required()).isEqualTo(10);
        }

        @Test
        @DisplayName("marks completed when initial >= required")
        void marksCompletedWhenInitialMeetsRequired() {
            Progress progress = Progress.of(10, 10);
            assertThat(progress.completed()).isTrue();
            assertThat(progress.completedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws on negative required")
        void throwsOnNegativeRequired() {
            assertThatThrownBy(() -> Progress.of(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("increment")
    class IncrementTests {

        @Test
        @DisplayName("increments progress")
        void incrementsProgress() {
            Progress progress = Progress.of(10);
            ProgressResult result = progress.increment(3);
            
            assertThat(result.progress().current()).isEqualTo(3);
            assertThat(result.justCompleted()).isFalse();
        }

        @Test
        @DisplayName("triggers completion when reaching required")
        void triggersCompletion() {
            Progress progress = Progress.of(5, 10);
            ProgressResult result = progress.increment(5);
            
            assertThat(result.progress().current()).isEqualTo(10);
            assertThat(result.progress().completed()).isTrue();
            assertThat(result.justCompleted()).isTrue();
        }

        @Test
        @DisplayName("clamps to required")
        void clampsToRequired() {
            Progress progress = Progress.of(10);
            ProgressResult result = progress.increment(100);
            
            assertThat(result.progress().current()).isEqualTo(10);
        }

        @Test
        @DisplayName("returns same progress when already completed")
        void returnsSameWhenCompleted() {
            Progress progress = Progress.of(10, 10);
            ProgressResult result = progress.increment(5);
            
            assertThat(result.progress()).isSameAs(progress);
            assertThat(result.justCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("calculations")
    class CalculationTests {

        @Test
        @DisplayName("calculates percentage correctly")
        void calculatesPercentage() {
            Progress progress = Progress.of(5, 10);
            assertThat(progress.percentage()).isEqualTo(0.5f);
            assertThat(progress.percentageInt()).isEqualTo(50);
        }

        @Test
        @DisplayName("returns 100% for zero required")
        void returns100ForZeroRequired() {
            Progress progress = Progress.of(0);
            assertThat(progress.percentage()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("calculates remaining correctly")
        void calculatesRemaining() {
            Progress progress = Progress.of(3, 10);
            assertThat(progress.remaining()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("formatting")
    class FormattingTests {

        @Test
        @DisplayName("formats incomplete progress")
        void formatsIncomplete() {
            Progress progress = Progress.of(5, 10);
            assertThat(progress.format()).isEqualTo("5/10");
        }

        @Test
        @DisplayName("formats complete progress with checkmark")
        void formatsComplete() {
            Progress progress = Progress.of(10, 10);
            assertThat(progress.format()).isEqualTo("10/10 ✓");
        }

        @Test
        @DisplayName("formats with percentage")
        void formatsWithPercentage() {
            Progress progress = Progress.of(5, 10);
            assertThat(progress.formatWithPercentage()).isEqualTo("5/10 (50%)");
        }
    }

    @Nested
    @DisplayName("reset")
    class ResetTests {

        @Test
        @DisplayName("resets to zero")
        void resetsToZero() {
            Progress progress = Progress.of(8, 10);
            Progress reset = progress.reset();
            
            assertThat(reset.current()).isEqualTo(0);
            assertThat(reset.required()).isEqualTo(10);
            assertThat(reset.completed()).isFalse();
        }
    }
}
