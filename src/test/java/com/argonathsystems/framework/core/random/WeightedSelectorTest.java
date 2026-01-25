package com.argonathsystems.framework.core.random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.*;

@DisplayName("WeightedSelector")
class WeightedSelectorTest {

    private WeightedSelector<String> selector;
    private RandomGenerator random;

    @BeforeEach
    void setUp() {
        selector = DefaultWeightedSelector.getInstance();
        random = RandomGenerator.getDefault();
    }

    @Nested
    @DisplayName("selectOne")
    class SelectOneTests {

        @Test
        @DisplayName("returns empty for empty pool")
        void returnsEmptyForEmptyPool() {
            Optional<String> result = selector.selectOne(List.of(), random);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty for null pool")
        void returnsEmptyForNullPool() {
            Optional<String> result = selector.selectOne(null, random);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when all weights are zero")
        void returnsEmptyWhenAllWeightsZero() {
            List<WeightedEntry<String>> entries = List.of(
                    WeightedEntry.of("a", 0),
                    WeightedEntry.of("b", 0)
            );
            Optional<String> result = selector.selectOne(entries, random);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns only item from single-entry pool")
        void returnsSingleItem() {
            List<WeightedEntry<String>> entries = List.of(
                    WeightedEntry.of("only", 1.0)
            );
            Optional<String> result = selector.selectOne(entries, random);
            assertThat(result).contains("only");
        }

        @Test
        @DisplayName("selects from pool with varying weights")
        void selectsFromVariedWeights() {
            List<WeightedEntry<String>> entries = List.of(
                    WeightedEntry.of("common", 100),
                    WeightedEntry.of("rare", 10),
                    WeightedEntry.of("epic", 1)
            );
            
            // Run multiple selections and verify they come from the pool
            for (int i = 0; i < 100; i++) {
                Optional<String> result = selector.selectOne(entries, random);
                assertThat(result).isPresent();
                assertThat(result.get()).isIn("common", "rare", "epic");
            }
        }
    }

    @Nested
    @DisplayName("selectMultiple")
    class SelectMultipleTests {

        @Test
        @DisplayName("returns empty list for count <= 0")
        void returnsEmptyForZeroCount() {
            List<WeightedEntry<String>> entries = List.of(
                    WeightedEntry.of("a", 1)
            );
            List<String> result = selector.selectMultiple(entries, 0, true, random);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns correct count with duplicates allowed")
        void returnsCorrectCountWithDuplicates() {
            List<WeightedEntry<String>> entries = List.of(
                    WeightedEntry.of("a", 1),
                    WeightedEntry.of("b", 1)
            );
            List<String> result = selector.selectMultiple(entries, 5, true, random);
            assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("returns unique items when duplicates not allowed")
        void returnsUniqueItems() {
            List<WeightedEntry<String>> entries = List.of(
                    WeightedEntry.of("a", 1),
                    WeightedEntry.of("b", 1),
                    WeightedEntry.of("c", 1)
            );
            List<String> result = selector.selectMultiple(entries, 3, false, random);
            assertThat(result).hasSize(3);
            assertThat(result).containsExactlyInAnyOrder("a", "b", "c");
        }

        @Test
        @DisplayName("limits to pool size when duplicates not allowed")
        void limitsToPoolSize() {
            List<WeightedEntry<String>> entries = List.of(
                    WeightedEntry.of("a", 1),
                    WeightedEntry.of("b", 1)
            );
            List<String> result = selector.selectMultiple(entries, 10, false, random);
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("selectOneWhere")
    class SelectOneWhereTests {

        @Test
        @DisplayName("filters entries before selection")
        void filtersEntries() {
            List<WeightedEntry<String>> entries = List.of(
                    WeightedEntry.of("apple", 100),
                    WeightedEntry.of("apricot", 100),
                    WeightedEntry.of("banana", 100)
            );
            
            // Only select items starting with 'a'
            for (int i = 0; i < 50; i++) {
                Optional<String> result = selector.selectOneWhere(
                        entries,
                        s -> s.startsWith("a"),
                        random
                );
                assertThat(result).isPresent();
                assertThat(result.get()).startsWith("a");
            }
        }

        @Test
        @DisplayName("returns empty when no entries match filter")
        void returnsEmptyWhenNoMatch() {
            List<WeightedEntry<String>> entries = List.of(
                    WeightedEntry.of("apple", 100),
                    WeightedEntry.of("banana", 100)
            );
            
            Optional<String> result = selector.selectOneWhere(
                    entries,
                    s -> s.startsWith("z"),
                    random
            );
            assertThat(result).isEmpty();
        }
    }
}
