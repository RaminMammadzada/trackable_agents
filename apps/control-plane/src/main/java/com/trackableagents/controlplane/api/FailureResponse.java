package com.trackableagents.controlplane.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record FailureResponse(
    String failureId,
    String runId,
    String eventId,
    String summary,
    JsonNode details,
    String status,
    Instant createdAt
) {
}

