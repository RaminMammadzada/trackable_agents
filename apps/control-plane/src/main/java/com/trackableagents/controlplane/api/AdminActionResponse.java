package com.trackableagents.controlplane.api;

import java.util.Map;

public record AdminActionResponse(
    String action,
    String message,
    Map<String, Long> counts
) {
}
