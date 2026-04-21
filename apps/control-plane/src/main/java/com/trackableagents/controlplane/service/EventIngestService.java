package com.trackableagents.controlplane.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.trackableagents.controlplane.api.AgentEventRequest;
import com.trackableagents.controlplane.api.IngestEventResponse;
import com.trackableagents.controlplane.api.RepoRefRequest;
import com.trackableagents.controlplane.config.AppProperties;
import com.trackableagents.controlplane.ledger.AgentEventEntity;
import com.trackableagents.controlplane.ledger.AgentEventRepository;
import com.trackableagents.controlplane.model.AgentRole;
import com.trackableagents.controlplane.model.EventSource;
import com.trackableagents.controlplane.model.EventType;
import com.trackableagents.controlplane.model.RiskLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class EventIngestService {

    private final AgentEventRepository agentEventRepository;
    private final ArtifactStorageService artifactStorageService;
    private final JsonMapper jsonMapper;
    private final RedactionService redactionService;
    private final RiskClassifier riskClassifier;
    private final ProjectionService projectionService;
    private final AppProperties properties;

    public EventIngestService(
        AgentEventRepository agentEventRepository,
        ArtifactStorageService artifactStorageService,
        JsonMapper jsonMapper,
        RedactionService redactionService,
        RiskClassifier riskClassifier,
        ProjectionService projectionService,
        AppProperties properties
    ) {
        this.agentEventRepository = agentEventRepository;
        this.artifactStorageService = artifactStorageService;
        this.jsonMapper = jsonMapper;
        this.redactionService = redactionService;
        this.riskClassifier = riskClassifier;
        this.projectionService = projectionService;
        this.properties = properties;
    }

    @Transactional
    public IngestEventResponse ingest(AgentEventRequest request) {
        AgentEventEntity existing = agentEventRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            return new IngestEventResponse(existing.getEventId(), existing.getRunId(), existing.getRiskLevel(), true);
        }

        EventSource source = EventSource.fromValue(request.source());
        AgentRole role = AgentRole.fromValue(request.agentRole());
        EventType eventType = EventType.fromValue(request.eventType());
        JsonNode redactedPayload = redactionService.redact(request.payload());
        String summary = redactionService.redactText(trimToLength(request.summary(), 500));
        RiskLevel computedRisk = riskClassifier.classify(summary, redactedPayload, eventType);
        RiskLevel effectiveRisk = request.riskLevel() == null
            ? computedRisk
            : RiskLevel.max(RiskLevel.fromValue(request.riskLevel()), computedRisk);

        String serializedPayload = jsonMapper.write(limitPayload(redactedPayload));
        AgentEventEntity entity = new AgentEventEntity();
        entity.setEventId(request.eventId() == null || request.eventId().isBlank() ? UUID.randomUUID().toString() : request.eventId());
        entity.setOccurredAt(request.occurredAt() == null ? Instant.now() : request.occurredAt());
        entity.setReceivedAt(request.receivedAt() == null ? Instant.now() : request.receivedAt());
        entity.setTraceId(request.traceId());
        entity.setSessionId(request.sessionId());
        entity.setRunId(request.runId());
        entity.setTaskId(request.taskId());
        entity.setSource(source.value());
        entity.setAgentRole(role.value());
        entity.setEventType(eventType.value());
        entity.setRiskLevel(effectiveRisk.name());
        entity.setSummary(summary);
        entity.setPayloadJson(serializedPayload);
        entity.setArtifactRefsJson(jsonMapper.write(
            redactionService.redact(jsonMapper.valueToTree(request.artifactRefs() == null ? java.util.List.of() : request.artifactRefs()))
        ));
        entity.setIdempotencyKey(request.idempotencyKey());

        applyRepoRef(entity, request.repoRef());

        agentEventRepository.save(entity);
        artifactStorageService.persistMetadata(request.artifactRefs(), entity.getEventId(), entity.getRunId());
        projectionService.rebuildForRun(entity.getRunId());
        return new IngestEventResponse(entity.getEventId(), entity.getRunId(), entity.getRiskLevel(), false);
    }

    private JsonNode limitPayload(JsonNode payload) {
        String serialized = jsonMapper.write(payload);
        long maxBytes = properties.getPayload().getMaxSizeBytes();
        if (serialized.getBytes().length <= maxBytes) {
            return payload;
        }
        return jsonMapper.objectNode()
            .put("truncated", true)
            .put("maxSizeBytes", maxBytes)
            .put("preview", trimToLength(serialized, (int) Math.min(maxBytes, 1024)));
    }

    private void applyRepoRef(AgentEventEntity entity, RepoRefRequest repoRef) {
        if (repoRef == null) {
            return;
        }
        entity.setRepoOwner(repoRef.owner());
        entity.setRepoName(repoRef.repo());
        entity.setRepoBranch(repoRef.branch());
        entity.setRepoCommit(repoRef.commit());
    }

    private String trimToLength(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
