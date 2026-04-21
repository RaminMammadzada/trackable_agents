package com.trackableagents.controlplane.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentEventRepository extends JpaRepository<AgentEventEntity, String> {
    Optional<AgentEventEntity> findByIdempotencyKey(String idempotencyKey);
    List<AgentEventEntity> findByRunIdOrderByOccurredAtAscReceivedAtAscEventIdAsc(String runId);
    List<AgentEventEntity> findTop200ByOrderByOccurredAtDescReceivedAtDesc();
}

