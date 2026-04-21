package com.trackableagents.controlplane.service;

import com.trackableagents.controlplane.ledger.AgentEventEntity;
import com.trackableagents.controlplane.model.RiskLevel;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProjectionSnapshot(
    String runId,
    String sessionId,
    String status,
    String source,
    RiskLevel riskLevel,
    Instant startedAt,
    Instant updatedAt,
    Instant completedAt,
    long eventCount,
    long failureCount,
    long lessonCount,
    String lastEventType,
    String lastSummary,
    String title,
    Map<String, TaskSnapshot> tasks,
    List<AgentEventEntity> orderedEvents
) {
    public record TaskSnapshot(
        String taskId,
        String runId,
        String status,
        RiskLevel riskLevel,
        Instant updatedAt,
        String lastEventType,
        String lastSummary
    ) {
    }
}

