package com.trackableagents.controlplane.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunProjectionRepository extends JpaRepository<RunProjectionEntity, String> {
    List<RunProjectionEntity> findTop100ByOrderByUpdatedAtDesc();
    long countByStatus(String status);
    long countBySource(String source);
}
