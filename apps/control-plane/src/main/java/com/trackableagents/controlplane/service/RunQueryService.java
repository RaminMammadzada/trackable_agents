package com.trackableagents.controlplane.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.trackableagents.controlplane.api.DashboardSummaryResponse;
import com.trackableagents.controlplane.api.EventResponse;
import com.trackableagents.controlplane.api.FailureResponse;
import com.trackableagents.controlplane.api.LessonResponse;
import com.trackableagents.controlplane.api.RepoRefRequest;
import com.trackableagents.controlplane.api.RunDetailResponse;
import com.trackableagents.controlplane.api.RunSummaryResponse;
import com.trackableagents.controlplane.learning.FailureRecordEntity;
import com.trackableagents.controlplane.learning.FailureRecordRepository;
import com.trackableagents.controlplane.learning.LessonProposalEntity;
import com.trackableagents.controlplane.learning.LessonProposalRepository;
import com.trackableagents.controlplane.ledger.AgentEventEntity;
import com.trackableagents.controlplane.ledger.AgentEventRepository;
import com.trackableagents.controlplane.projection.RunProjectionEntity;
import com.trackableagents.controlplane.projection.RunProjectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RunQueryService {

    private final RunProjectionRepository runProjectionRepository;
    private final AgentEventRepository agentEventRepository;
    private final FailureRecordRepository failureRecordRepository;
    private final LessonProposalRepository lessonProposalRepository;
    private final JsonMapper jsonMapper;

    public RunQueryService(
        RunProjectionRepository runProjectionRepository,
        AgentEventRepository agentEventRepository,
        FailureRecordRepository failureRecordRepository,
        LessonProposalRepository lessonProposalRepository,
        JsonMapper jsonMapper
    ) {
        this.runProjectionRepository = runProjectionRepository;
        this.agentEventRepository = agentEventRepository;
        this.failureRecordRepository = failureRecordRepository;
        this.lessonProposalRepository = lessonProposalRepository;
        this.jsonMapper = jsonMapper;
    }

    public List<RunSummaryResponse> listRuns() {
        return runProjectionRepository.findTop100ByOrderByUpdatedAtDesc().stream()
            .map(this::toRunSummary)
            .toList();
    }

    public RunDetailResponse getRun(String runId) {
        RunProjectionEntity run = runProjectionRepository.findById(runId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown runId: " + runId));
        List<EventResponse> events = agentEventRepository.findByRunIdOrderByOccurredAtAscReceivedAtAscEventIdAsc(runId).stream()
            .map(this::toEvent)
            .toList();
        List<FailureResponse> failures = failureRecordRepository.findByRunIdOrderByCreatedAtDesc(runId).stream()
            .map(this::toFailure)
            .toList();
        List<LessonResponse> lessons = lessonProposalRepository.findByRunIdOrderByCreatedAtDesc(runId).stream()
            .map(this::toLesson)
            .toList();
        return new RunDetailResponse(toRunSummary(run), events, failures, lessons);
    }

    public List<EventResponse> getRunEvents(String runId) {
        return agentEventRepository.findByRunIdOrderByOccurredAtAscReceivedAtAscEventIdAsc(runId).stream()
            .map(this::toEvent)
            .toList();
    }

    public List<FailureResponse> listFailures() {
        return failureRecordRepository.findTop100ByOrderByCreatedAtDesc().stream()
            .map(this::toFailure)
            .toList();
    }

    public List<LessonResponse> listLessons() {
        return lessonProposalRepository.findTop100ByOrderByCreatedAtDesc().stream()
            .map(this::toLesson)
            .toList();
    }

    public DashboardSummaryResponse dashboardSummary() {
        List<RunProjectionEntity> runs = runProjectionRepository.findAll();
        long activeRuns = runs.stream().filter(run -> "active".equals(run.getStatus())).count();
        long attentionRequired = runs.stream().filter(run -> "attention_required".equals(run.getStatus())).count();
        long codex = runs.stream().filter(run -> "codex".equals(run.getSource())).count();
        long claude = runs.stream().filter(run -> "claude".equals(run.getSource())).count();
        long copilot = runs.stream().filter(run -> "copilot".equals(run.getSource()) || "github".equals(run.getSource())).count();
        long pendingLessons = lessonProposalRepository.findAll().stream().filter(lesson -> "pending".equals(lesson.getStatus())).count();
        return new DashboardSummaryResponse(
            runs.size(),
            activeRuns,
            attentionRequired,
            failureRecordRepository.count(),
            pendingLessons,
            codex,
            claude,
            copilot
        );
    }

    private RunSummaryResponse toRunSummary(RunProjectionEntity run) {
        return new RunSummaryResponse(
            run.getRunId(),
            run.getSessionId(),
            run.getStatus(),
            run.getSource(),
            run.getRiskLevel(),
            run.getTitle(),
            run.getEventCount(),
            run.getFailureCount(),
            run.getLessonCount(),
            run.getLastEventType(),
            run.getLastSummary(),
            run.getStartedAt(),
            run.getUpdatedAt(),
            run.getCompletedAt()
        );
    }

    private EventResponse toEvent(AgentEventEntity event) {
        JsonNode payload = jsonMapper.read(event.getPayloadJson());
        JsonNode artifactRefs = jsonMapper.read(event.getArtifactRefsJson());
        return new EventResponse(
            event.getEventId(),
            event.getOccurredAt(),
            event.getReceivedAt(),
            event.getTraceId(),
            event.getSessionId(),
            event.getRunId(),
            event.getTaskId(),
            event.getSource(),
            event.getAgentRole(),
            event.getEventType(),
            event.getRiskLevel(),
            new RepoRefRequest(event.getRepoOwner(), event.getRepoName(), event.getRepoBranch(), event.getRepoCommit()),
            event.getSummary(),
            payload,
            artifactRefs
        );
    }

    private FailureResponse toFailure(FailureRecordEntity failure) {
        return new FailureResponse(
            failure.getFailureId(),
            failure.getRunId(),
            failure.getEventId(),
            failure.getSummary(),
            jsonMapper.read(failure.getDetailsJson()),
            failure.getStatus(),
            failure.getCreatedAt()
        );
    }

    private LessonResponse toLesson(LessonProposalEntity lesson) {
        return new LessonResponse(
            lesson.getLessonId(),
            lesson.getRunId(),
            lesson.getFailureId(),
            lesson.getTitle(),
            lesson.getSummary(),
            jsonMapper.read(lesson.getDetailsJson()),
            lesson.getStatus(),
            lesson.getDecisionNote(),
            lesson.getCreatedAt(),
            lesson.getDecidedAt()
        );
    }
}

