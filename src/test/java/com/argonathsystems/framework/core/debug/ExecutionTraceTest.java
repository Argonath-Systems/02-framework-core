package com.argonathsystems.framework.core.debug;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ExecutionTrace")
class ExecutionTraceTest {

    @Test
    @DisplayName("creates trace with operation name")
    void createsWithName() {
        ExecutionTrace trace = new ExecutionTrace("test-op");
        assertThat(trace.getOperationName()).isEqualTo("test-op");
    }

    @Test
    @DisplayName("records success state")
    void recordsSuccess() {
        ExecutionTrace trace = ExecutionTrace.start("test")
                .end(true);
        assertThat(trace.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("records failure state")
    void recordsFailure() {
        ExecutionTrace trace = ExecutionTrace.start("test")
                .end(false);
        assertThat(trace.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("records result value")
    void recordsResult() {
        ExecutionTrace trace = ExecutionTrace.start("test")
                .end(true, "result value");
        assertThat(trace.getResult()).isEqualTo("result value");
    }

    @Test
    @DisplayName("sets description")
    void setsDescription() {
        ExecutionTrace trace = ExecutionTrace.start("test")
                .description("A test operation")
                .end(true);
        assertThat(trace.getDescription()).isEqualTo("A test operation");
    }

    @Test
    @DisplayName("adds child traces")
    void addsChildren() {
        ExecutionTrace parent = ExecutionTrace.start("parent");
        ExecutionTrace child1 = parent.child("child1").end(true);
        ExecutionTrace child2 = parent.child("child2").end(false);
        parent.end(true);
        
        assertThat(parent.getChildren()).hasSize(2);
        assertThat(parent.getChildren().get(0).getOperationName()).isEqualTo("child1");
    }

    @Test
    @DisplayName("stores metadata")
    void storesMetadata() {
        ExecutionTrace trace = ExecutionTrace.start("test")
                .metadata("key1", "value1")
                .metadata("key2", 42)
                .end(true);
        
        assertThat(trace.getMetadata())
                .containsEntry("key1", "value1")
                .containsEntry("key2", 42);
    }

    @Test
    @DisplayName("converts to map")
    void convertsToMap() {
        ExecutionTrace trace = ExecutionTrace.start("test")
                .description("test desc")
                .metadata("key", "value")
                .end(true, "result");
        
        Map<String, Object> map = trace.toMap();
        assertThat(map.get("operation")).isEqualTo("test");
        assertThat(map.get("success")).isEqualTo(true);
        assertThat(map.get("description")).isEqualTo("test desc");
        assertThat(map.get("result")).isEqualTo("result");
    }

    @Test
    @DisplayName("generates pretty string")
    void generatesPrettyString() {
        ExecutionTrace trace = ExecutionTrace.start("parent")
                .description("parent op");
        trace.child("child").end(true, "child result");
        trace.end(true);
        
        String pretty = trace.toPrettyString();
        assertThat(pretty).contains("parent");
        assertThat(pretty).contains("child");
        assertThat(pretty).contains("✓");
    }
}
