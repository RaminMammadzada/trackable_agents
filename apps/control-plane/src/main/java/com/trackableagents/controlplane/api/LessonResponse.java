package com.trackableagents.controlplane.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record LessonResponse(
    String lessonId,
    String runId,
    String failureId,
    String title,
    String summary,
    JsonNode details,
    String status,
    String decisionNote,
    Instant createdAt,
    Instant decidedAt
) {
}

