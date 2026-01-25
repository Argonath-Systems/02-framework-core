package com.argonathsystems.framework.core.random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("WeightedEntry")
class WeightedEntryTest {

    @Test
    @DisplayName("creates entry with valid weight")
    void createsWithValidWeight() {
        WeightedEntry<String> entry = WeightedEntry.of("item", 10.5);
        assertThat(entry.item()).isEqualTo("item");
        assertThat(entry.weight()).isEqualTo(10.5);
    }

    @Test
    @DisplayName("allows zero weight")
    void allowsZeroWeight() {
        WeightedEntry<String> entry = WeightedEntry.of("item", 0);
        assertThat(entry.weight()).isEqualTo(0);
    }

    @Test
    @DisplayName("throws on negative weight")
    void throwsOnNegativeWeight() {
        assertThatThrownBy(() -> WeightedEntry.of("item", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    @DisplayName("withWeight creates new entry")
    void withWeightCreatesNew() {
        WeightedEntry<String> original = WeightedEntry.of("item", 10);
        WeightedEntry<String> modified = original.withWeight(20);
        
        assertThat(modified.item()).isEqualTo("item");
        assertThat(modified.weight()).isEqualTo(20);
        assertThat(original.weight()).isEqualTo(10);
    }

    @Test
    @DisplayName("scaleWeight multiplies correctly")
    void scaleWeightMultiplies() {
        WeightedEntry<String> entry = WeightedEntry.of("item", 10);
        WeightedEntry<String> scaled = entry.scaleWeight(2.5);
        
        assertThat(scaled.weight()).isEqualTo(25);
    }
}
