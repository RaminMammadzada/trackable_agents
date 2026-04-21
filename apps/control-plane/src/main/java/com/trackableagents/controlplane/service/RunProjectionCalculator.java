package com.trackableagents.controlplane.service;

import com.trackableagents.controlplane.ledger.AgentEventEntity;
import com.trackableagents.controlplane.model.EventType;
import com.trackableagents.controlplane.model.RiskLevel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RunProjectionCalculator {

    public ProjectionSnapshot calculate(String runId, List<AgentEventEntity> orderedEvents) {
        if (orderedEvents.isEmpty()) {
            throw new IllegalArgumentException("Cannot build a projection without events");
        }

        AgentEventEntity first = orderedEvents.getFirst();
        AgentEventEntity last = orderedEvents.getLast();

        String sessionId = null;
        String source = first.getSource();
        RiskLevel highestRisk = RiskLevel.R0;
        long failureCount = 0;
        long lessonCount = 0;
        Instant completedAt = null;
        String status = "active";
        Map<String, ProjectionSnapshot.TaskSnapshot> tasks = new LinkedHashMap<>();

        for (AgentEventEntity event : orderedEvents) {
            sessionId = sessionId == null && event.getSessionId() != null ? event.getSessionId() : sessionId;
            highestRisk = RiskLevel.max(highestRisk, RiskLevel.fromValue(event.getRiskLevel()));
            source = event.getSource();

            EventType type = EventType.fromValue(event.getEventType());
            if (type == EventType.FAILURE_RECORDED) {
                failureCount++;
                status = "attention_required";
            } else if (type == EventType.LESSON_PROPOSED) {
                lessonCount++;
            } else if (type == EventType.APPROVAL_REQUESTED) {
                status = "awaiting_approval";
            } else if (type == EventType.RUN_COMPLETED || type == EventType.SESSION_ENDED) {
                completedAt = event.getOccurredAt();
                status = "completed";
            }

            if (event.getTaskId() != null && !event.getTaskId().isBlank()) {
                tasks.put(event.getTaskId(), new ProjectionSnapshot.TaskSnapshot(
                    event.getTaskId(),
                    runId,
                    mapTaskStatus(type),
                    RiskLevel.fromValue(event.getRiskLevel()),
                    event.getOccurredAt(),
                    event.getEventType(),
                    event.getSummary()
                ));
            }
        }

        if ("active".equals(status) && highestRisk.severity() >= RiskLevel.R4.severity()) {
            status = "high_risk";
        }

        return new ProjectionSnapshot(
            runId,
            sessionId,
            status,
            source,
            highestRisk,
            first.getOccurredAt(),
            last.getOccurredAt(),
            completedAt,
            orderedEvents.size(),
            failureCount,
            lessonCount,
            last.getEventType(),
            last.getSummary(),
            first.getSummary(),
            tasks,
            orderedEvents
        );
    }

    private String mapTaskStatus(EventType eventType) {
        return switch (eventType) {
            case TEST_FINISHED, REVIEW_COMPLETED -> "completed";
            case FAILURE_RECORDED -> "failed";
            case APPROVAL_REQUESTED -> "awaiting_approval";
            default -> "active";
        };
    }
}

