package com.trackableagents.controlplane.service;

import com.trackableagents.controlplane.ledger.AgentEventEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RunProjectionCalculatorTest {

    private final RunProjectionCalculator calculator = new RunProjectionCalculator();

    @Test
    void computesProjectionFromOutOfOrderEventStreamOnceSorted() {
        AgentEventEntity started = event("evt-1", "run-1", "session.started", "R0", "codex", Instant.parse("2026-04-21T10:00:00Z"), "Start");
        AgentEventEntity failure = event("evt-2", "run-1", "failure.recorded", "R3", "codex", Instant.parse("2026-04-21T10:05:00Z"), "Failure");
        AgentEventEntity completed = event("evt-3", "run-1", "run.completed", "R2", "codex", Instant.parse("2026-04-21T10:06:00Z"), "Done");

        ProjectionSnapshot snapshot = calculator.calculate("run-1", List.of(started, failure, completed));

        assertThat(snapshot.status()).isEqualTo("completed");
        assertThat(snapshot.failureCount()).isEqualTo(1);
        assertThat(snapshot.riskLevel().name()).isEqualTo("R3");
        assertThat(snapshot.completedAt()).isEqualTo(Instant.parse("2026-04-21T10:06:00Z"));
    }

    private AgentEventEntity event(String eventId, String runId, String eventType, String risk, String source, Instant occurredAt, String summary) {
        AgentEventEntity entity = new AgentEventEntity();
        entity.setEventId(eventId);
        entity.setRunId(runId);
        entity.setEventType(eventType);
        entity.setRiskLevel(risk);
        entity.setSource(source);
        entity.setAgentRole("implementation");
        entity.setSummary(summary);
        entity.setOccurredAt(occurredAt);
        entity.setReceivedAt(occurredAt);
        entity.setPayloadJson("{}");
        entity.setArtifactRefsJson("[]");
        entity.setIdempotencyKey(eventId);
        return entity;
    }
}

