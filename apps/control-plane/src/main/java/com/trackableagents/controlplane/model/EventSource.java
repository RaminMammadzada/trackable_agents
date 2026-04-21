package com.trackableagents.controlplane.model;

import java.util.Arrays;

public enum EventSource {
    CODEX("codex"),
    CLAUDE("claude"),
    COPILOT("copilot"),
    GITHUB("github");

    private final String value;

    EventSource(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static EventSource fromValue(String value) {
        return Arrays.stream(values())
            .filter(source -> source.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported source: " + value));
    }
}

