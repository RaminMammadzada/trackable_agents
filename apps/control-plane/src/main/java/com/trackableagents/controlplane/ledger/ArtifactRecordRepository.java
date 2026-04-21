package com.trackableagents.controlplane.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtifactRecordRepository extends JpaRepository<ArtifactRecordEntity, String> {
    List<ArtifactRecordEntity> findByRunId(String runId);
}

