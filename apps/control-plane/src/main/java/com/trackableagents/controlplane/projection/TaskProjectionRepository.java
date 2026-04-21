package com.trackableagents.controlplane.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskProjectionRepository extends JpaRepository<TaskProjectionEntity, String> {
    List<TaskProjectionEntity> findByRunIdOrderByUpdatedAtDesc(String runId);
}

