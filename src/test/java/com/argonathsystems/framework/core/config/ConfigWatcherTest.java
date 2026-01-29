package com.argonathsystems.framework.core.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ConfigWatcher.
 */
@DisplayName("ConfigWatcher")
class ConfigWatcherTest {

    @TempDir
    Path tempDir;

    private ConfigWatcher watcher;

    @BeforeEach
    void setUp() throws IOException {
        watcher = ConfigWatcher.builder()
                .debounceMillis(50)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (watcher != null) {
            watcher.shutdown();
        }
    }

    // =========================================================================
    // Creation Tests
    // =========================================================================

    @Nested
    @DisplayName("Creation")
    class CreationTests {

        @Test
        @DisplayName("creates with default settings")
        void createsWithDefaults() throws IOException {
            try (ConfigWatcher defaultWatcher = ConfigWatcher.create()) {
                assertThat(defaultWatcher.isRunning()).isTrue();
                assertThat(defaultWatcher.watchedFileCount()).isZero();
            }
        }

        @Test
        @DisplayName("creates with custom debounce time")
        void createsWithCustomDebounce() throws IOException {
            try (ConfigWatcher customWatcher = ConfigWatcher.builder()
                    .debounceTime(Duration.ofMillis(200))
                    .build()) {
                assertThat(customWatcher.isRunning()).isTrue();
            }
        }
    }

    // =========================================================================
    // Watch Tests
    // =========================================================================

    @Nested
    @DisplayName("watch()")
    class WatchTests {

        @Test
        @DisplayName("registers file for watching")
        void registersFile() throws IOException {
            Path configFile = tempDir.resolve("config.yml");
            Files.writeString(configFile, "test: value");

            watcher.watch(configFile, path -> {});

            assertThat(watcher.isWatching(configFile)).isTrue();
            assertThat(watcher.watchedFileCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("throws for non-existent file")
        void throwsForNonExistent() {
            Path nonExistent = tempDir.resolve("does-not-exist.yml");

            assertThatThrownBy(() -> watcher.watch(nonExistent, path -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not exist");
        }

        @Test
        @DisplayName("throws for directory path")
        void throwsForDirectory() {
            assertThatThrownBy(() -> watcher.watch(tempDir, path -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a regular file");
        }

        @Test
        @DisplayName("throws for null path")
        void throwsForNullPath() {
            assertThatThrownBy(() -> watcher.watch(null, path -> {}))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws for null callback")
        void throwsForNullCallback() throws IOException {
            Path configFile = tempDir.resolve("config.yml");
            Files.writeString(configFile, "test: value");

            assertThatThrownBy(() -> watcher.watch(configFile, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // =========================================================================
    // Unwatch Tests
    // =========================================================================

    @Nested
    @DisplayName("unwatch()")
    class UnwatchTests {

        @Test
        @DisplayName("removes file from watching")
        void removesFile() throws IOException {
            Path configFile = tempDir.resolve("config.yml");
            Files.writeString(configFile, "test: value");

            watcher.watch(configFile, path -> {});
            assertThat(watcher.isWatching(configFile)).isTrue();

            watcher.unwatch(configFile);
            assertThat(watcher.isWatching(configFile)).isFalse();
            assertThat(watcher.watchedFileCount()).isZero();
        }

        @Test
        @DisplayName("does nothing for non-watched file")
        void doesNothingForNonWatched() throws IOException {
            Path configFile = tempDir.resolve("config.yml");
            Files.writeString(configFile, "test: value");

            // Should not throw
            watcher.unwatch(configFile);

            assertThat(watcher.watchedFileCount()).isZero();
        }
    }

    // =========================================================================
    // File Change Detection Tests
    // =========================================================================

    @Nested
    @DisplayName("File Change Detection")
    class FileChangeTests {

        @Test
        @DisplayName("triggers callback on file modification")
        void triggersCallbackOnModification() throws Exception {
            Path configFile = tempDir.resolve("config.yml");
            Files.writeString(configFile, "initial: value");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Path> changedPath = new AtomicReference<>();

            watcher.watch(configFile, path -> {
                changedPath.set(path);
                latch.countDown();
            });

            // Small delay to ensure watch is registered
            Thread.sleep(100);

            // Modify the file
            Files.writeString(configFile, "modified: value");

            boolean triggered = latch.await(5, TimeUnit.SECONDS);

            assertThat(triggered).isTrue();
            assertThat(changedPath.get()).isEqualTo(configFile.toAbsolutePath().normalize());
        }

        @Test
        @DisplayName("debounces rapid changes")
        void debouncesRapidChanges() throws Exception {
            Path configFile = tempDir.resolve("config.yml");
            Files.writeString(configFile, "initial: value");

            AtomicInteger callCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(1);

            watcher.watch(configFile, path -> {
                callCount.incrementAndGet();
                latch.countDown();
            });

            // Small delay to ensure watch is registered
            Thread.sleep(100);

            // Rapid modifications
            for (int i = 0; i < 5; i++) {
                Files.writeString(configFile, "change: " + i);
                Thread.sleep(10); // Faster than debounce time
            }

            // Wait for potential callbacks
            Thread.sleep(200);

            // Should have been debounced to 1 or 2 callbacks, not 5
            assertThat(callCount.get()).isLessThanOrEqualTo(2);
        }
    }

    // =========================================================================
    // Multiple File Tests
    // =========================================================================

    @Nested
    @DisplayName("Multiple Files")
    class MultipleFileTests {

        @Test
        @DisplayName("watches multiple files independently")
        void watchesMultipleFiles() throws Exception {
            Path file1 = tempDir.resolve("config1.yml");
            Path file2 = tempDir.resolve("config2.yml");
            Files.writeString(file1, "file: 1");
            Files.writeString(file2, "file: 2");

            CountDownLatch latch1 = new CountDownLatch(1);
            CountDownLatch latch2 = new CountDownLatch(1);

            watcher.watch(file1, path -> latch1.countDown());
            watcher.watch(file2, path -> latch2.countDown());

            assertThat(watcher.watchedFileCount()).isEqualTo(2);

            // Small delay to ensure watches are registered
            Thread.sleep(100);

            // Modify only file1
            Files.writeString(file1, "modified: 1");

            boolean triggered1 = latch1.await(5, TimeUnit.SECONDS);

            assertThat(triggered1).isTrue();
            // latch2 should not have been triggered
            assertThat(latch2.getCount()).isEqualTo(1);
        }
    }

    // =========================================================================
    // Shutdown Tests
    // =========================================================================

    @Nested
    @DisplayName("Shutdown")
    class ShutdownTests {

        @Test
        @DisplayName("stops running after shutdown")
        void stopsAfterShutdown() throws IOException {
            Path configFile = tempDir.resolve("config.yml");
            Files.writeString(configFile, "test: value");

            watcher.watch(configFile, path -> {});

            watcher.shutdown();

            assertThat(watcher.isRunning()).isFalse();
        }

        @Test
        @DisplayName("clears watched files on shutdown")
        void clearsWatchedFilesOnShutdown() throws IOException {
            Path configFile = tempDir.resolve("config.yml");
            Files.writeString(configFile, "test: value");

            watcher.watch(configFile, path -> {});
            assertThat(watcher.watchedFileCount()).isEqualTo(1);

            watcher.shutdown();

            assertThat(watcher.watchedFileCount()).isZero();
        }

        @Test
        @DisplayName("shutdown is idempotent")
        void shutdownIsIdempotent() {
            watcher.shutdown();
            watcher.shutdown(); // Should not throw

            assertThat(watcher.isRunning()).isFalse();
        }

        @Test
        @DisplayName("close() calls shutdown")
        void closeCallsShutdown() throws Exception {
            ConfigWatcher autoCloseWatcher = ConfigWatcher.create();

            try (autoCloseWatcher) {
                assertThat(autoCloseWatcher.isRunning()).isTrue();
            }

            assertThat(autoCloseWatcher.isRunning()).isFalse();
        }
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("handles callback exception gracefully")
        void handlesCallbackException() throws Exception {
            Path configFile = tempDir.resolve("config.yml");
            Files.writeString(configFile, "initial: value");

            CountDownLatch successLatch = new CountDownLatch(1);
            AtomicInteger callCount = new AtomicInteger(0);

            watcher.watch(configFile, path -> {
                int count = callCount.incrementAndGet();
                if (count == 1) {
                    throw new RuntimeException("Test exception");
                }
                successLatch.countDown();
            });

            // Small delay to ensure watch is registered
            Thread.sleep(100);

            // First modification - throws
            Files.writeString(configFile, "change: 1");
            Thread.sleep(150); // Wait past debounce

            // Second modification - should still work
            Files.writeString(configFile, "change: 2");

            boolean triggered = successLatch.await(5, TimeUnit.SECONDS);

            // Watcher should still be running despite exception
            assertThat(watcher.isRunning()).isTrue();
            assertThat(triggered).isTrue();
        }
    }
}
