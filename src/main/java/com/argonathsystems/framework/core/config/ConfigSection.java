package com.argonathsystems.framework.core.config;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Abstraction over configuration sections.
 * Works with YAML, JSON, or any hierarchical config format.
 */
public interface ConfigSection {

    // === Primitive Getters ===

    /**
     * Get a string value.
     *
     * @param path The path to the value
     * @return The string value, or null if not found
     */
    String getString(String path);

    /**
     * Get a string value with default.
     *
     * @param path         The path to the value
     * @param defaultValue Default if not found
     * @return The string value or default
     */
    String getString(String path, String defaultValue);

    /**
     * Get an integer value.
     *
     * @param path The path to the value
     * @return The int value, or 0 if not found
     */
    int getInt(String path);

    /**
     * Get an integer value with default.
     *
     * @param path         The path to the value
     * @param defaultValue Default if not found
     * @return The int value or default
     */
    int getInt(String path, int defaultValue);

    /**
     * Get a long value.
     *
     * @param path The path to the value
     * @return The long value, or 0 if not found
     */
    long getLong(String path);

    /**
     * Get a long value with default.
     *
     * @param path         The path to the value
     * @param defaultValue Default if not found
     * @return The long value or default
     */
    long getLong(String path, long defaultValue);

    /**
     * Get a double value.
     *
     * @param path The path to the value
     * @return The double value, or 0.0 if not found
     */
    double getDouble(String path);

    /**
     * Get a double value with default.
     *
     * @param path         The path to the value
     * @param defaultValue Default if not found
     * @return The double value or default
     */
    double getDouble(String path, double defaultValue);

    /**
     * Get a boolean value.
     *
     * @param path The path to the value
     * @return The boolean value, or false if not found
     */
    boolean getBoolean(String path);

    /**
     * Get a boolean value with default.
     *
     * @param path         The path to the value
     * @param defaultValue Default if not found
     * @return The boolean value or default
     */
    boolean getBoolean(String path, boolean defaultValue);

    // === Complex Types ===

    /**
     * Get a list of strings.
     *
     * @param path The path to the list
     * @return List of strings, or empty list if not found
     */
    List<String> getStringList(String path);

    /**
     * Get a list of integers.
     *
     * @param path The path to the list
     * @return List of integers, or empty list if not found
     */
    List<Integer> getIntList(String path);

    /**
     * Get a nested config section.
     *
     * @param path The path to the section
     * @return The nested section, or null if not found
     */
    ConfigSection getSection(String path);

    /**
     * Get a list of config sections.
     *
     * @param path The path to the section list
     * @return List of sections, or empty list if not found
     */
    List<ConfigSection> getSectionList(String path);

    /**
     * Get top-level keys.
     *
     * @return Set of keys at this level
     */
    Set<String> getKeys();

    /**
     * Get keys, optionally including nested keys.
     *
     * @param deep If true, include nested keys with dot notation
     * @return Set of keys
     */
    Set<String> getKeys(boolean deep);

    /**
     * Check if a path exists.
     *
     * @param path The path to check
     * @return true if the path exists
     */
    boolean contains(String path);

    // === Type Conversion ===

    /**
     * Get a value with type conversion.
     *
     * @param path The path to the value
     * @param type The class to convert to
     * @param <T>  The target type
     * @return The converted value, or null if not found
     */
    <T> T get(String path, Class<T> type);

    /**
     * Get a value with type conversion and default.
     *
     * @param path         The path to the value
     * @param type         The class to convert to
     * @param defaultValue Default if not found
     * @param <T>          The target type
     * @return The converted value or default
     */
    <T> T get(String path, Class<T> type, T defaultValue);

    /**
     * Get a value as Optional.
     *
     * @param path The path to the value
     * @param type The class to convert to
     * @param <T>  The target type
     * @return Optional containing the value, or empty
     */
    <T> Optional<T> getOptional(String path, Class<T> type);
}
