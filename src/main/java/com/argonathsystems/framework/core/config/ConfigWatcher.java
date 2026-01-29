package com.argonathsystems.framework.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Watches configuration files for changes and triggers callbacks.
 * Provides hot-reload capability for configuration files.
 *
 * <p>Features:
 * <ul>
 *     <li>Automatic debouncing to prevent rapid fire callbacks</li>
 *     <li>Thread-safe callback execution</li>
 *     <li>Multiple file watching with independent callbacks</li>
 *     <li>Graceful shutdown with resource cleanup</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * ConfigWatcher watcher = ConfigWatcher.builder()
 *     .debounceTime(Duration.ofMillis(200))
 *     .build();
 *
 * watcher.watch(configPath, path -> {
 *     System.out.println("Config changed: " + path);
 *     reloadConfig();
 * });
 *
 * // Later, when shutting down:
 * watcher.shutdown();
 * }</pre>
 *
 * @author Argonath Systems Team
 * @version 2.1.0
 * @since 2.1.0
 */
public final class ConfigWatcher implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigWatcher.class);
    private static final Duration DEFAULT_DEBOUNCE = Duration.ofMillis(100);

    private final WatchService watchService;
    private final Duration debounceTime;
    private final ExecutorService executor;
    private final Map<Path, WatchedFile> watchedFiles;
    private final Map<WatchKey, Path> watchKeyToDirectory;
    private final AtomicBoolean running;
    private final Thread watchThread;

    private ConfigWatcher(Duration debounceTime) throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.debounceTime = debounceTime;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ConfigWatcher-Callback");
            t.setDaemon(true);
            return t;
        });
        this.watchedFiles = new ConcurrentHashMap<>();
        this.watchKeyToDirectory = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(true);
        this.watchThread = new Thread(this::watchLoop, "ConfigWatcher-Watch");
        this.watchThread.setDaemon(true);
        this.watchThread.start();

        LOGGER.info("ConfigWatcher started with debounce time: {}ms", debounceTime.toMillis());
    }

    /**
     * Create a new ConfigWatcher builder.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a ConfigWatcher with default settings.
     *
     * @return A new ConfigWatcher
     * @throws IOException if the watch service cannot be created
     */
    public static ConfigWatcher create() throws IOException {
        return new ConfigWatcher(DEFAULT_DEBOUNCE);
    }

    /**
     * Watch a file for changes.
     *
     * @param path     Path to the file to watch
     * @param callback Callback to invoke when the file changes
     * @throws IOException          if the path cannot be watched
     * @throws IllegalArgumentException if the path is not a regular file
     */
    public void watch(Path path, Consumer<Path> callback) throws IOException {
        Objects.requireNonNull(path, "Path cannot be null");
        Objects.requireNonNull(callback, "Callback cannot be null");

        Path absolutePath = path.toAbsolutePath().normalize();

        if (!Files.exists(absolutePath)) {
            throw new IllegalArgumentException("File does not exist: " + absolutePath);
        }

        if (!Files.isRegularFile(absolutePath)) {
            throw new IllegalArgumentException("Path is not a regular file: " + absolutePath);
        }

        Path directory = absolutePath.getParent();
        String fileName = absolutePath.getFileName().toString();

        // Register the directory if not already registered
        if (!watchKeyToDirectory.containsValue(directory)) {
            WatchKey key = directory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE
            );
            watchKeyToDirectory.put(key, directory);
            LOGGER.debug("Registered watch on directory: {}", directory);
        }

        // Register the file callback
        watchedFiles.put(absolutePath, new WatchedFile(fileName, callback, Instant.EPOCH));
        LOGGER.info("Watching file for changes: {}", absolutePath);
    }

    /**
     * Stop watching a specific file.
     *
     * @param path Path to stop watching
     */
    public void unwatch(Path path) {
        Path absolutePath = path.toAbsolutePath().normalize();
        WatchedFile removed = watchedFiles.remove(absolutePath);
        if (removed != null) {
            LOGGER.info("Stopped watching file: {}", absolutePath);
        }
    }

    /**
     * Check if a file is being watched.
     *
     * @param path Path to check
     * @return true if the file is being watched
     */
    public boolean isWatching(Path path) {
        return watchedFiles.containsKey(path.toAbsolutePath().normalize());
    }

    /**
     * Get the number of files being watched.
     *
     * @return Number of watched files
     */
    public int watchedFileCount() {
        return watchedFiles.size();
    }

    /**
     * Shutdown the watcher and release resources.
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            LOGGER.info("Shutting down ConfigWatcher...");

            try {
                watchService.close();
            } catch (IOException e) {
                LOGGER.warn("Error closing watch service", e);
            }

            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            watchedFiles.clear();
            watchKeyToDirectory.clear();

            LOGGER.info("ConfigWatcher shutdown complete");
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    /**
     * Check if the watcher is running.
     *
     * @return true if still watching for changes
     */
    public boolean isRunning() {
        return running.get();
    }

    // =========================================================================
    // Internal Implementation
    // =========================================================================

    private void watchLoop() {
        LOGGER.debug("Watch loop started");

        while (running.get()) {
            WatchKey key;
            try {
                key = watchService.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }

            if (key == null) {
                continue;
            }

            Path directory = watchKeyToDirectory.get(key);
            if (directory == null) {
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path fileName = pathEvent.context();
                Path fullPath = directory.resolve(fileName);

                handleFileEvent(fullPath);
            }

            boolean valid = key.reset();
            if (!valid) {
                watchKeyToDirectory.remove(key);
                LOGGER.warn("Watch key invalidated for directory: {}", directory);
            }
        }

        LOGGER.debug("Watch loop ended");
    }

    private void handleFileEvent(Path fullPath) {
        WatchedFile watchedFile = watchedFiles.get(fullPath);
        if (watchedFile == null) {
            return;
        }

        Instant now = Instant.now();
        Duration sinceLastEvent = Duration.between(watchedFile.lastEvent(), now);

        if (sinceLastEvent.compareTo(debounceTime) < 0) {
            LOGGER.trace("Debouncing event for: {} ({}ms since last)", fullPath, sinceLastEvent.toMillis());
            return;
        }

        // Update last event time
        watchedFiles.put(fullPath, watchedFile.withLastEvent(now));

        LOGGER.debug("File changed: {}", fullPath);

        // Execute callback asynchronously
        executor.submit(() -> {
            try {
                watchedFile.callback().accept(fullPath);
            } catch (Exception e) {
                LOGGER.error("Error in file change callback for: {}", fullPath, e);
            }
        });
    }

    // =========================================================================
    // Internal Data Structures
    // =========================================================================

    private record WatchedFile(
            String fileName,
            Consumer<Path> callback,
            Instant lastEvent
    ) {
        WatchedFile withLastEvent(Instant newLastEvent) {
            return new WatchedFile(fileName, callback, newLastEvent);
        }
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Builder for ConfigWatcher.
     */
    public static final class Builder {

        private Duration debounceTime = DEFAULT_DEBOUNCE;

        private Builder() {
        }

        /**
         * Set the debounce time for file change events.
         * Events within this duration of each other are coalesced into one.
         *
         * @param debounceTime Minimum time between callbacks for the same file
         * @return This builder
         */
        public Builder debounceTime(Duration debounceTime) {
            this.debounceTime = Objects.requireNonNull(debounceTime, "Debounce time cannot be null");
            return this;
        }

        /**
         * Set debounce time in milliseconds.
         *
         * @param millis Debounce time in milliseconds
         * @return This builder
         */
        public Builder debounceMillis(long millis) {
            this.debounceTime = Duration.ofMillis(millis);
            return this;
        }

        /**
         * Build the ConfigWatcher.
         *
         * @return A new ConfigWatcher instance
         * @throws IOException if the watch service cannot be created
         */
        public ConfigWatcher build() throws IOException {
            return new ConfigWatcher(debounceTime);
        }
    }
}
