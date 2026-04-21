package com.trackableagents.controlplane.api;

public record IngestEventResponse(
    String eventId,
    String runId,
    String riskLevel,
    boolean duplicate
) {
}

