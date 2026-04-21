package com.trackableagents.controlplane.api;

import java.time.Instant;

public record RunSummaryResponse(
    String runId,
    String sessionId,
    String status,
    String source,
    String riskLevel,
    String title,
    long eventCount,
    long failureCount,
    long lessonCount,
    String lastEventType,
    String lastSummary,
    Instant startedAt,
    Instant updatedAt,
    Instant completedAt
) {
}

