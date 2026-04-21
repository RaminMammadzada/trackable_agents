package com.trackableagents.controlplane.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trackableagents.controlplane.api.AdminActionResponse;
import com.trackableagents.controlplane.api.AgentEventRequest;
import com.trackableagents.controlplane.api.ArtifactRefRequest;
import com.trackableagents.controlplane.api.RepoRefRequest;
import com.trackableagents.controlplane.learning.FailureRecordRepository;
import com.trackableagents.controlplane.learning.LessonProposalRepository;
import com.trackableagents.controlplane.ledger.AgentEventRepository;
import com.trackableagents.controlplane.ledger.ArtifactRecordRepository;
import com.trackableagents.controlplane.projection.RunProjectionRepository;
import com.trackableagents.controlplane.projection.SessionProjectionRepository;
import com.trackableagents.controlplane.projection.TaskProjectionRepository;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminRuntimeService {

    private final AgentEventRepository agentEventRepository;
    private final ArtifactRecordRepository artifactRecordRepository;
    private final RunProjectionRepository runProjectionRepository;
    private final SessionProjectionRepository sessionProjectionRepository;
    private final TaskProjectionRepository taskProjectionRepository;
    private final FailureRecordRepository failureRecordRepository;
    private final LessonProposalRepository lessonProposalRepository;
    private final ArtifactStorageService artifactStorageService;
    private final EventIngestService eventIngestService;
    private final JsonMapper jsonMapper;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public AdminRuntimeService(
        AgentEventRepository agentEventRepository,
        ArtifactRecordRepository artifactRecordRepository,
        RunProjectionRepository runProjectionRepository,
        SessionProjectionRepository sessionProjectionRepository,
        TaskProjectionRepository taskProjectionRepository,
        FailureRecordRepository failureRecordRepository,
        LessonProposalRepository lessonProposalRepository,
        ArtifactStorageService artifactStorageService,
        EventIngestService eventIngestService,
        JsonMapper jsonMapper,
        JdbcTemplate jdbcTemplate,
        PlatformTransactionManager transactionManager
    ) {
        this.agentEventRepository = agentEventRepository;
        this.artifactRecordRepository = artifactRecordRepository;
        this.runProjectionRepository = runProjectionRepository;
        this.sessionProjectionRepository = sessionProjectionRepository;
        this.taskProjectionRepository = taskProjectionRepository;
        this.failureRecordRepository = failureRecordRepository;
        this.lessonProposalRepository = lessonProposalRepository;
        this.artifactStorageService = artifactStorageService;
        this.eventIngestService = eventIngestService;
        this.jsonMapper = jsonMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public AdminActionResponse seedDemoData() {
        String prefix = "demo-" + Instant.now().toEpochMilli();
        List<AgentEventRequest> requests = List.of(
            event(prefix, 0, "codex", "implementation", "session.started", "R1",
                "Codex implementation session started",
                repoRef("trackable_agents", "feature/codex-observer", "68234cf"),
                payload("sessionLabel", "codex-success", "operator", "local"),
                "codex-session-success", "codex-run-success", "task-refactor", List.of()),
            event(prefix, 1, "codex", "implementation", "prompt.submitted", "R2",
                "Implement append-only event projection refresh",
                repoRef("trackable_agents", "feature/codex-observer", "68234cf"),
                payload("prompt", "Refresh projections after each persisted event", "files", List.of("ProjectionService.java")),
                "codex-session-success", "codex-run-success", "task-refactor", List.of()),
            event(prefix, 2, "codex", "implementation", "tool.post_call", "R2",
                "Updated projection rebuild path and verified app tests",
                repoRef("trackable_agents", "feature/codex-observer", "7ee6083"),
                payload("tool", "exec_command", "command", "./mvnw test", "exitCode", 0),
                "codex-session-success", "codex-run-success", "task-refactor", List.of()),
            event(prefix, 3, "codex", "test", "test.finished", "R1",
                "Control plane test suite passed",
                repoRef("trackable_agents", "feature/codex-observer", "7ee6083"),
                payload("suite", "control-plane", "passed", true, "tests", 12),
                "codex-session-success", "codex-run-success", "task-refactor", List.of()),
            event(prefix, 4, "codex", "implementation", "run.completed", "R1",
                "Codex run completed cleanly",
                repoRef("trackable_agents", "feature/codex-observer", "aac3837"),
                payload("outcome", "success", "changedFiles", 4),
                "codex-session-success", "codex-run-success", "task-refactor", List.of()),

            event(prefix, 10, "claude", "implementation", "session.started", "R1",
                "Claude debugging session started",
                repoRef("trackable_agents", "bugfix/hook-runtime", "aac3837"),
                payload("sessionLabel", "claude-failure", "operator", "local"),
                "claude-session-failure", "claude-run-failure", "task-fix", List.of()),
            event(prefix, 11, "claude", "implementation", "tool.pre_call", "R3",
                "Preparing to run integration tests against local Postgres",
                repoRef("trackable_agents", "bugfix/hook-runtime", "aac3837"),
                payload("tool", "docker compose", "command", "docker compose up --build"),
                "claude-session-failure", "claude-run-failure", "task-fix", List.of()),
            event(prefix, 12, "claude", "test", "failure.recorded", "R4",
                "Webhook replay produced a duplicate run projection and failed reconciliation",
                repoRef("trackable_agents", "bugfix/hook-runtime", "aac3837"),
                payload("failureMode", "duplicate_projection", "nextAction", "scope run ids by backend", "secret", "ghp_demo_should_redact"),
                "claude-session-failure", "claude-run-failure", "task-fix",
                List.of(new ArtifactRefRequest("failure-log", "artifacts/demo/claude-failure.log", "text/plain", "sha256-demo-failure", 2048L))),
            event(prefix, 13, "claude", "review", "lesson.proposed", "R2",
                "Scope run ids by backend and session when normalizing hook traffic",
                repoRef("trackable_agents", "bugfix/hook-runtime", "aac3837"),
                payload("lessonType", "normalization", "prevents", List.of("projection collisions", "ambiguous timelines")),
                "claude-session-failure", "claude-run-failure", "task-fix", List.of()),
            event(prefix, 14, "claude", "implementation", "run.completed", "R2",
                "Claude run completed with follow-up required",
                repoRef("trackable_agents", "bugfix/hook-runtime", "aac3837"),
                payload("outcome", "attention_required", "failures", 1),
                "claude-session-failure", "claude-run-failure", "task-fix", List.of()),

            event(prefix, 20, "copilot", "review", "pr.created", "R2",
                "Copilot opened a pull request for dashboard polish",
                repoRef("trackable_agents", "feature/copilot-ui", "demo-pr-123"),
                payload("prNumber", 17, "title", "Polish dashboard empty states", "branch", "feature/copilot-ui"),
                "copilot-session-review", "copilot-run-pr", "task-dashboard", List.of()),
            event(prefix, 21, "github", "review", "review.completed", "R2",
                "GitHub review completed with one requested change",
                repoRef("trackable_agents", "feature/copilot-ui", "demo-pr-123"),
                payload("reviewState", "changes_requested", "comments", 1),
                "copilot-session-review", "copilot-run-pr", "task-dashboard", List.of()),
            event(prefix, 22, "copilot", "docs", "run.completed", "R1",
                "Copilot PR run recorded for dashboard monitoring",
                repoRef("trackable_agents", "feature/copilot-ui", "demo-pr-123"),
                payload("outcome", "observed", "checks", "pending"),
                "copilot-session-review", "copilot-run-pr", "task-dashboard", List.of())
        );

        requests.forEach(eventIngestService::ingest);

        return new AdminActionResponse(
            "seed-demo-data",
            "Loaded a local demo dataset with Codex, Claude, and Copilot/GitHub activity.",
            Map.of(
                "eventsCreated", (long) requests.size(),
                "runsCreated", 3L,
                "sessionsCreated", 3L
            )
        );
    }

    public AdminActionResponse resetRuntimeData() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("lessonsDeleted", lessonProposalRepository.count());
        counts.put("failuresDeleted", failureRecordRepository.count());
        counts.put("tasksDeleted", taskProjectionRepository.count());
        counts.put("sessionsDeleted", sessionProjectionRepository.count());
        counts.put("runsDeleted", runProjectionRepository.count());
        counts.put("artifactsDeleted", artifactRecordRepository.count());
        counts.put("eventsDeleted", agentEventRepository.count());

        truncateRuntimeTablesWithRetry();

        counts.put("artifactFilesDeleted", (long) artifactStorageService.clearRuntimeArtifacts());

        return new AdminActionResponse(
            "reset-runtime-data",
            "Cleared local runtime data tables and artifact files. Migrations and code remain untouched.",
            counts
        );
    }

    private void truncateRuntimeTablesWithRetry() {
        int attempts = 0;
        while (true) {
            try {
                transactionTemplate.executeWithoutResult(status -> jdbcTemplate.execute("""
                    TRUNCATE TABLE
                        lesson_proposals,
                        failure_records,
                        task_projections,
                        session_projections,
                        run_projections,
                        artifact_records,
                        agent_events
                    """));
                return;
            } catch (TransientDataAccessException exception) {
                attempts += 1;
                if (attempts >= 5) {
                    throw exception;
                }
                try {
                    Thread.sleep(200L * attempts);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while retrying runtime reset", interrupted);
                }
            }
        }
    }

    private AgentEventRequest event(
        String prefix,
        long secondsOffset,
        String source,
        String agentRole,
        String eventType,
        String riskLevel,
        String summary,
        RepoRefRequest repoRef,
        ObjectNode payload,
        String sessionId,
        String runId,
        String taskId,
        List<ArtifactRefRequest> artifactRefs
    ) {
        Instant occurredAt = Instant.now().plusSeconds(secondsOffset);
        return new AgentEventRequest(
            UUID.randomUUID().toString(),
            occurredAt,
            occurredAt.plusMillis(250),
            prefix + "-trace-" + runId,
            prefix + "-" + sessionId,
            prefix + "-" + runId,
            prefix + "-" + taskId,
            source,
            agentRole,
            eventType,
            riskLevel,
            repoRef,
            summary,
            payload,
            artifactRefs,
            prefix + "-" + runId + "-" + eventType + "-" + secondsOffset
        );
    }

    private RepoRefRequest repoRef(String repo, String branch, String commit) {
        return new RepoRefRequest("RaminMammadzada", repo, branch, commit);
    }

    private ObjectNode payload(Object... keyValues) {
        ObjectNode node = jsonMapper.objectNode();
        for (int index = 0; index < keyValues.length; index += 2) {
            String key = String.valueOf(keyValues[index]);
            Object value = keyValues[index + 1];
            node.set(key, jsonMapper.valueToTree(value));
        }
        return node;
    }
}
