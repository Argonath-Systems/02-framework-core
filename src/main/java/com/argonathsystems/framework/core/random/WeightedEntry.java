package com.argonathsystems.framework.core.random;

/**
 * Entry in a weighted pool.
 *
 * @param item   The item in the pool
 * @param weight The weight determining selection probability (must be non-negative)
 * @param <T>    The type of the item
 */
public record WeightedEntry<T>(T item, double weight) {

    /**
     * Creates a weighted entry.
     *
     * @throws IllegalArgumentException if weight is negative
     */
    public WeightedEntry {
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative: " + weight);
        }
    }

    /**
     * Factory method to create a weighted entry.
     *
     * @param item   The item
     * @param weight The weight
     * @param <T>    The item type
     * @return A new WeightedEntry
     */
    public static <T> WeightedEntry<T> of(T item, double weight) {
        return new WeightedEntry<>(item, weight);
    }

    /**
     * Create entry with modified weight.
     *
     * @param newWeight The new weight
     * @return A new WeightedEntry with the same item but different weight
     */
    public WeightedEntry<T> withWeight(double newWeight) {
        return new WeightedEntry<>(item, newWeight);
    }

    /**
     * Scale weight by a factor.
     *
     * @param factor The factor to multiply the weight by
     * @return A new WeightedEntry with scaled weight
     */
    public WeightedEntry<T> scaleWeight(double factor) {
        return new WeightedEntry<>(item, weight * factor);
    }
}
