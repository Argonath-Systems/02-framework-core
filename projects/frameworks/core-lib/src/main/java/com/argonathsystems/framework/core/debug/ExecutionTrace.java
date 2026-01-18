package com.argonathsystems.framework.core.debug;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traces execution of operations for debugging.
 * Useful for understanding why conditions evaluated a certain way,
 * or why loot tables generated specific results.
 */
public class ExecutionTrace {

    private final String operationName;
    private final Instant startTime;
    private Instant endTime;
    private boolean success;
    private Object result;
    private String description;
    private final List<ExecutionTrace> children;
    private final Map<String, Object> metadata;

    /**
     * Create a new execution trace.
     *
     * @param operationName Name of the operation being traced
     */
    public ExecutionTrace(String operationName) {
        this.operationName = operationName;
        this.startTime = Instant.now();
        this.children = new ArrayList<>();
        this.metadata = new LinkedHashMap<>();
    }

    /**
     * Create and start a new trace.
     *
     * @param operationName Name of the operation
     * @return A new ExecutionTrace
     */
    public static ExecutionTrace start(String operationName) {
        return new ExecutionTrace(operationName);
    }

    /**
     * End the trace.
     *
     * @param success Whether the operation succeeded
     * @return This trace for chaining
     */
    public ExecutionTrace end(boolean success) {
        this.endTime = Instant.now();
        this.success = success;
        return this;
    }

    /**
     * End the trace with a result.
     *
     * @param success Whether the operation succeeded
     * @param result  The result of the operation
     * @return This trace for chaining
     */
    public ExecutionTrace end(boolean success, Object result) {
        this.result = result;
        return end(success);
    }

    /**
     * Set a description for this trace.
     *
     * @param description Human-readable description
     * @return This trace for chaining
     */
    public ExecutionTrace description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Add a child trace.
     *
     * @param child The child trace
     * @return This trace for chaining
     */
    public ExecutionTrace addChild(ExecutionTrace child) {
        children.add(child);
        return this;
    }

    /**
     * Create and add a child trace.
     *
     * @param operationName Name of the child operation
     * @return The new child trace
     */
    public ExecutionTrace child(String operationName) {
        ExecutionTrace child = new ExecutionTrace(operationName);
        children.add(child);
        return child;
    }

    /**
     * Add metadata to this trace.
     *
     * @param key   Metadata key
     * @param value Metadata value
     * @return This trace for chaining
     */
    public ExecutionTrace metadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * Get the duration of this operation.
     *
     * @return Duration from start to end, or zero if not ended
     */
    public Duration getDuration() {
        if (endTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(startTime, endTime);
    }

    /**
     * Get the operation name.
     *
     * @return The operation name
     */
    public String getOperationName() {
        return operationName;
    }

    /**
     * Check if the operation succeeded.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Get the result of the operation.
     *
     * @return The result, or null
     */
    public Object getResult() {
        return result;
    }

    /**
     * Get the description.
     *
     * @return The description, or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get child traces.
     *
     * @return List of child traces
     */
    public List<ExecutionTrace> getChildren() {
        return children;
    }

    /**
     * Get metadata.
     *
     * @return Metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Format as pretty tree string.
     *
     * @return Formatted string representation
     */
    public String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        toPrettyString(sb, "", true);
        return sb.toString();
    }

    private void toPrettyString(StringBuilder sb, String prefix, boolean isLast) {
        String icon = success ? "✓" : "✗";
        String duration = formatDuration(getDuration());

        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(icon).append(" ").append(operationName);
        sb.append(" (").append(duration).append(")");

        if (description != null) {
            sb.append(" - ").append(description);
        }

        if (result != null) {
            sb.append(" → ").append(result);
        }

        sb.append("\n");

        // Print metadata
        if (!metadata.isEmpty()) {
            String metaPrefix = prefix + (isLast ? "    " : "│   ");
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                sb.append(metaPrefix).append("  ").append(entry.getKey())
                        .append(": ").append(entry.getValue()).append("\n");
            }
        }

        // Print children
        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < children.size(); i++) {
            children.get(i).toPrettyString(sb, childPrefix, i == children.size() - 1);
        }
    }

    private String formatDuration(Duration duration) {
        long nanos = duration.toNanos();
        if (nanos < 1_000) {
            return nanos + "ns";
        } else if (nanos < 1_000_000) {
            return String.format("%.1fμs", nanos / 1000.0);
        } else if (nanos < 1_000_000_000) {
            return String.format("%.2fms", nanos / 1_000_000.0);
        } else {
            return String.format("%.2fs", nanos / 1_000_000_000.0);
        }
    }

    /**
     * Convert to structured map for logging/serialization.
     *
     * @return Map representation of this trace
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("operation", operationName);
        map.put("success", success);
        map.put("durationNanos", getDuration().toNanos());

        if (description != null) {
            map.put("description", description);
        }
        if (result != null) {
            map.put("result", result.toString());
        }
        if (!metadata.isEmpty()) {
            map.put("metadata", metadata);
        }

        if (!children.isEmpty()) {
            map.put("children", children.stream().map(ExecutionTrace::toMap).toList());
        }

        return map;
    }

    @Override
    public String toString() {
        return toPrettyString();
    }
}
