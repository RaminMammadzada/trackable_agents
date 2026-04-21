package com.trackableagents.controlplane.model;

import java.util.Arrays;

public enum AgentRole {
    HUMAN("human"),
    PLANNER("planner"),
    IMPLEMENTATION("implementation"),
    TEST("test"),
    REVIEW("review"),
    SECURITY("security"),
    DOCS("docs"),
    UNKNOWN("unknown");

    private final String value;

    AgentRole(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static AgentRole fromValue(String value) {
        return Arrays.stream(values())
            .filter(role -> role.value.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported agentRole: " + value));
    }
}

