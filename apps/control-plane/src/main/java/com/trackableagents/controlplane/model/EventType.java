package com.trackableagents.controlplane.model;

import java.util.Arrays;

public enum EventType {
    SESSION_STARTED("session.started"),
    SESSION_ENDED("session.ended"),
    PROMPT_SUBMITTED("prompt.submitted"),
    TOOL_PRE_CALL("tool.pre_call"),
    TOOL_POST_CALL("tool.post_call"),
    FILE_CHANGED("file.changed"),
    TEST_STARTED("test.started"),
    TEST_FINISHED("test.finished"),
    RISK_DETECTED("risk.detected"),
    APPROVAL_REQUESTED("approval.requested"),
    APPROVAL_RESOLVED("approval.resolved"),
    PR_CREATED("pr.created"),
    REVIEW_COMPLETED("review.completed"),
    FAILURE_RECORDED("failure.recorded"),
    LESSON_PROPOSED("lesson.proposed"),
    RUN_COMPLETED("run.completed");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static EventType fromValue(String value) {
        return Arrays.stream(values())
            .filter(type -> type.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported eventType: " + value));
    }
}

