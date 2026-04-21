package com.trackableagents.controlplane.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public record LessonProposalRequest(
    @NotBlank String runId,
    String failureId,
    @NotBlank String title,
    @NotBlank String summary,
    JsonNode details
) {
}

