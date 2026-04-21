package com.trackableagents.controlplane.service;

import com.trackableagents.controlplane.api.ArtifactRefRequest;
import com.trackableagents.controlplane.config.AppProperties;
import com.trackableagents.controlplane.ledger.ArtifactRecordEntity;
import com.trackableagents.controlplane.ledger.ArtifactRecordRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ArtifactStorageService {

    private final AppProperties properties;
    private final ArtifactRecordRepository artifactRecordRepository;

    public ArtifactStorageService(AppProperties properties, ArtifactRecordRepository artifactRecordRepository) {
        this.properties = properties;
        this.artifactRecordRepository = artifactRecordRepository;
    }

    public void persistMetadata(List<ArtifactRefRequest> artifacts, String eventId, String runId) {
        if (artifacts == null || artifacts.isEmpty()) {
            return;
        }

        Path root = Path.of(properties.getStorage().getArtifactRoot()).normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create artifact root at " + root, exception);
        }

        for (ArtifactRefRequest artifact : artifacts) {
            ArtifactRecordEntity entity = new ArtifactRecordEntity();
            entity.setArtifactId(UUID.randomUUID().toString());
            entity.setEventId(eventId);
            entity.setRunId(runId);
            entity.setLabel(artifact.label());
            entity.setStoragePath(artifact.path());
            entity.setMimeType(artifact.mimeType());
            entity.setChecksum(artifact.checksum());
            entity.setSizeBytes(artifact.sizeBytes());
            entity.setCreatedAt(Instant.now());
            artifactRecordRepository.save(entity);
        }
    }
}

