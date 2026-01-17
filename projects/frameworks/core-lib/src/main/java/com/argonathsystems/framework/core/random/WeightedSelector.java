package com.argonathsystems.framework.core.random;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Selects items from a weighted pool.
 * Thread-safe and reusable.
 *
 * @param <T> The type of items in the pool
 */
public interface WeightedSelector<T> {

    /**
     * Select one item based on weights.
     *
     * @param entries Weighted entries to select from
     * @param random  Random generator to use
     * @return Selected item, or empty if pool is empty or all weights are zero
     */
    Optional<T> selectOne(List<WeightedEntry<T>> entries, RandomGenerator random);

    /**
     * Select multiple items.
     *
     * @param entries         Pool to select from
     * @param count           Number of items to select
     * @param allowDuplicates Whether the same item can be selected multiple times
     * @param random          Random generator
     * @return List of selected items (may be smaller than count if pool exhausted)
     */
    List<T> selectMultiple(
            List<WeightedEntry<T>> entries,
            int count,
            boolean allowDuplicates,
            RandomGenerator random
    );

    /**
     * Select one item, filtering by predicate first.
     *
     * @param entries Pool to select from
     * @param filter  Predicate to filter entries before selection
     * @param random  Random generator
     * @return Selected item, or empty if no matching entries
     */
    Optional<T> selectOneWhere(
            List<WeightedEntry<T>> entries,
            Predicate<T> filter,
            RandomGenerator random
    );
}
