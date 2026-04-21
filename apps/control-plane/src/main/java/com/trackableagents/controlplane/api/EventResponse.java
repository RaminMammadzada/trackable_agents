package com.trackableagents.controlplane.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record EventResponse(
    String eventId,
    Instant occurredAt,
    Instant receivedAt,
    String traceId,
    String sessionId,
    String runId,
    String taskId,
    String source,
    String agentRole,
    String eventType,
    String riskLevel,
    RepoRefRequest repoRef,
    String summary,
    JsonNode payload,
    JsonNode artifactRefs
) {
}

