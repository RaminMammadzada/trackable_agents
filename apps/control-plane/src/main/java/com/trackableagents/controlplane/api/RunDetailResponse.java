package com.trackableagents.controlplane.api;

import java.util.List;

public record RunDetailResponse(
    RunSummaryResponse run,
    List<EventResponse> events,
    List<FailureResponse> failures,
    List<LessonResponse> lessons
) {
}

