package com.trackableagents.controlplane.model;

import java.util.Arrays;

public enum RiskLevel {
    R0(0),
    R1(1),
    R2(2),
    R3(3),
    R4(4),
    R5(5);

    private final int severity;

    RiskLevel(int severity) {
        this.severity = severity;
    }

    public int severity() {
        return severity;
    }

    public static RiskLevel fromValue(String value) {
        return Arrays.stream(values())
            .filter(level -> level.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported riskLevel: " + value));
    }

    public static RiskLevel max(RiskLevel left, RiskLevel right) {
        return left.severity >= right.severity ? left : right;
    }
}

