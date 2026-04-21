package com.trackableagents.controlplane.learning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonProposalRepository extends JpaRepository<LessonProposalEntity, String> {
    List<LessonProposalEntity> findTop100ByOrderByCreatedAtDesc();
    List<LessonProposalEntity> findByRunIdOrderByCreatedAtDesc(String runId);
}

