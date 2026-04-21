package com.trackableagents.controlplane.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FailureRecordRepository extends JpaRepository<FailureRecordEntity, String> {
    List<FailureRecordEntity> findTop100ByOrderByCreatedAtDesc();
    List<FailureRecordEntity> findByRunIdOrderByCreatedAtDesc(String runId);
    Optional<FailureRecordEntity> findByEventId(String eventId);
    long countByStatus(String status);
}
