package com.trackableagents.controlplane.service;

import com.trackableagents.controlplane.learning.FailureRecordEntity;
import com.trackableagents.controlplane.learning.FailureRecordRepository;
import com.trackableagents.controlplane.learning.LessonProposalEntity;
import com.trackableagents.controlplane.learning.LessonProposalRepository;
import com.trackableagents.controlplane.ledger.AgentEventEntity;
import com.trackableagents.controlplane.ledger.AgentEventRepository;
import com.trackableagents.controlplane.model.EventType;
import com.trackableagents.controlplane.projection.RunProjectionEntity;
import com.trackableagents.controlplane.projection.RunProjectionRepository;
import com.trackableagents.controlplane.projection.SessionProjectionEntity;
import com.trackableagents.controlplane.projection.SessionProjectionRepository;
import com.trackableagents.controlplane.projection.TaskProjectionEntity;
import com.trackableagents.controlplane.projection.TaskProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectionService {

    private final AgentEventRepository agentEventRepository;
    private final RunProjectionRepository runProjectionRepository;
    private final SessionProjectionRepository sessionProjectionRepository;
    private final TaskProjectionRepository taskProjectionRepository;
    private final FailureRecordRepository failureRecordRepository;
    private final LessonProposalRepository lessonProposalRepository;
    private final RunProjectionCalculator runProjectionCalculator;

    public ProjectionService(
        AgentEventRepository agentEventRepository,
        RunProjectionRepository runProjectionRepository,
        SessionProjectionRepository sessionProjectionRepository,
        TaskProjectionRepository taskProjectionRepository,
        FailureRecordRepository failureRecordRepository,
        LessonProposalRepository lessonProposalRepository,
        RunProjectionCalculator runProjectionCalculator
    ) {
        this.agentEventRepository = agentEventRepository;
        this.runProjectionRepository = runProjectionRepository;
        this.sessionProjectionRepository = sessionProjectionRepository;
        this.taskProjectionRepository = taskProjectionRepository;
        this.failureRecordRepository = failureRecordRepository;
        this.lessonProposalRepository = lessonProposalRepository;
        this.runProjectionCalculator = runProjectionCalculator;
    }

    @Transactional
    public void rebuildForRun(String runId) {
        if (runId == null || runId.isBlank()) {
            return;
        }

        List<AgentEventEntity> events = agentEventRepository.findByRunIdOrderByOccurredAtAscReceivedAtAscEventIdAsc(runId);
        if (events.isEmpty()) {
            return;
        }

        ProjectionSnapshot snapshot = runProjectionCalculator.calculate(runId, events);

        RunProjectionEntity runProjection = runProjectionRepository.findById(runId).orElseGet(RunProjectionEntity::new);
        runProjection.setRunId(runId);
        runProjection.setSessionId(snapshot.sessionId());
        runProjection.setStatus(snapshot.status());
        runProjection.setSource(snapshot.source());
        runProjection.setRiskLevel(snapshot.riskLevel().name());
        runProjection.setStartedAt(snapshot.startedAt());
        runProjection.setUpdatedAt(snapshot.updatedAt());
        runProjection.setCompletedAt(snapshot.completedAt());
        runProjection.setEventCount(snapshot.eventCount());
        runProjection.setFailureCount(snapshot.failureCount());
        runProjection.setLessonCount(snapshot.lessonCount());
        runProjection.setLastEventType(snapshot.lastEventType());
        runProjection.setLastSummary(snapshot.lastSummary());
        runProjection.setTitle(snapshot.title());
        runProjectionRepository.save(runProjection);

        if (snapshot.sessionId() != null && !snapshot.sessionId().isBlank()) {
            SessionProjectionEntity sessionProjection = sessionProjectionRepository.findById(snapshot.sessionId())
                .orElseGet(SessionProjectionEntity::new);
            sessionProjection.setSessionId(snapshot.sessionId());
            sessionProjection.setStatus(snapshot.completedAt() == null ? "active" : "completed");
            sessionProjection.setStartedAt(snapshot.startedAt());
            sessionProjection.setUpdatedAt(snapshot.updatedAt());
            sessionProjection.setEndedAt(snapshot.completedAt());
            sessionProjection.setRunCount(runProjectionRepository.findAll().stream()
                .filter(run -> snapshot.sessionId().equals(run.getSessionId()))
                .count());
            sessionProjection.setRiskLevel(snapshot.riskLevel().name());
            sessionProjectionRepository.save(sessionProjection);
        }

        for (ProjectionSnapshot.TaskSnapshot taskSnapshot : snapshot.tasks().values()) {
            TaskProjectionEntity taskProjection = taskProjectionRepository.findById(taskSnapshot.taskId())
                .orElseGet(TaskProjectionEntity::new);
            taskProjection.setTaskId(taskSnapshot.taskId());
            taskProjection.setRunId(taskSnapshot.runId());
            taskProjection.setStatus(taskSnapshot.status());
            taskProjection.setRiskLevel(taskSnapshot.riskLevel().name());
            taskProjection.setUpdatedAt(taskSnapshot.updatedAt());
            taskProjection.setLastEventType(taskSnapshot.lastEventType());
            taskProjection.setLastSummary(taskSnapshot.lastSummary());
            taskProjectionRepository.save(taskProjection);
        }

        for (AgentEventEntity event : events) {
            EventType eventType = EventType.fromValue(event.getEventType());
            if (eventType == EventType.FAILURE_RECORDED && failureRecordRepository.findByEventId(event.getEventId()).isEmpty()) {
                FailureRecordEntity failure = new FailureRecordEntity();
                failure.setFailureId(UUID.randomUUID().toString());
                failure.setRunId(runId);
                failure.setEventId(event.getEventId());
                failure.setSummary(event.getSummary());
                failure.setDetailsJson(event.getPayloadJson());
                failure.setStatus("open");
                failure.setCreatedAt(event.getOccurredAt());
                failureRecordRepository.save(failure);
            }

            if (eventType == EventType.LESSON_PROPOSED && lessonProposalRepository.findByRunIdOrderByCreatedAtDesc(runId).stream()
                .noneMatch(lesson -> lesson.getSummary().equals(event.getSummary()))) {
                LessonProposalEntity lesson = new LessonProposalEntity();
                lesson.setLessonId(UUID.randomUUID().toString());
                lesson.setRunId(runId);
                lesson.setTitle(event.getSummary());
                lesson.setSummary(event.getSummary());
                lesson.setDetailsJson(event.getPayloadJson());
                lesson.setStatus("pending");
                lesson.setCreatedAt(event.getOccurredAt());
                lessonProposalRepository.save(lesson);
            }
        }
    }
}
