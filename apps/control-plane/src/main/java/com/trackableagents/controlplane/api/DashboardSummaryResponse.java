package com.trackableagents.controlplane.api;

public record DashboardSummaryResponse(
    long totalRuns,
    long activeRuns,
    long attentionRequiredRuns,
    long totalFailures,
    long pendingLessons,
    long codexRuns,
    long claudeRuns,
    long copilotRuns
) {
}

