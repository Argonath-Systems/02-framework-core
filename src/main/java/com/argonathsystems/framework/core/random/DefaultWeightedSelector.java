package com.argonathsystems.framework.core.random;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Default implementation of WeightedSelector.
 * Thread-safe and stateless - uses a singleton pattern.
 *
 * @param <T> The type of items in the pool
 */
public class DefaultWeightedSelector<T> implements WeightedSelector<T> {

    private static final DefaultWeightedSelector<?> INSTANCE = new DefaultWeightedSelector<>();

    /**
     * Gets the singleton instance.
     *
     * @param <T> The type of items
     * @return The singleton WeightedSelector instance
     */
    @SuppressWarnings("unchecked")
    public static <T> WeightedSelector<T> getInstance() {
        return (WeightedSelector<T>) INSTANCE;
    }

    @Override
    public Optional<T> selectOne(List<WeightedEntry<T>> entries, RandomGenerator random) {
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        double totalWeight = entries.stream()
                .mapToDouble(WeightedEntry::weight)
                .sum();

        if (totalWeight <= 0) {
            return Optional.empty();
        }

        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0;

        for (WeightedEntry<T> entry : entries) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return Optional.of(entry.item());
            }
        }

        // Fallback for floating-point edge cases
        return Optional.of(entries.getLast().item());
    }

    @Override
    public List<T> selectMultiple(
            List<WeightedEntry<T>> entries,
            int count,
            boolean allowDuplicates,
            RandomGenerator random) {

        if (entries == null || entries.isEmpty() || count <= 0) {
            return List.of();
        }

        List<T> results = new ArrayList<>(count);

        if (allowDuplicates) {
            for (int i = 0; i < count; i++) {
                selectOne(entries, random).ifPresent(results::add);
            }
        } else {
            List<WeightedEntry<T>> remaining = new ArrayList<>(entries);
            for (int i = 0; i < count && !remaining.isEmpty(); i++) {
                Optional<T> selected = selectOne(remaining, random);
                if (selected.isPresent()) {
                    T item = selected.get();
                    results.add(item);
                    remaining.removeIf(e -> e.item().equals(item));
                }
            }
        }

        return results;
    }

    @Override
    public Optional<T> selectOneWhere(
            List<WeightedEntry<T>> entries,
            Predicate<T> filter,
            RandomGenerator random) {

        if (entries == null || entries.isEmpty() || filter == null) {
            return Optional.empty();
        }

        List<WeightedEntry<T>> filtered = entries.stream()
                .filter(e -> filter.test(e.item()))
                .toList();

        return selectOne(filtered, random);
    }
}
