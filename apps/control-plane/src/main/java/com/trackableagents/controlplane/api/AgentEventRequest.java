package com.trackableagents.controlplane.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record AgentEventRequest(
    String eventId,
    Instant occurredAt,
    Instant receivedAt,
    String traceId,
    String sessionId,
    String runId,
    String taskId,
    @NotBlank String source,
    @NotBlank String agentRole,
    @NotBlank String eventType,
    String riskLevel,
    @Valid RepoRefRequest repoRef,
    @NotBlank String summary,
    JsonNode payload,
    @Valid List<ArtifactRefRequest> artifactRefs,
    @NotNull String idempotencyKey
) {
}

