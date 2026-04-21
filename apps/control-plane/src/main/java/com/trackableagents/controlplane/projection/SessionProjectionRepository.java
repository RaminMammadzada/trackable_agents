package com.trackableagents.controlplane.projection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionProjectionRepository extends JpaRepository<SessionProjectionEntity, String> {
}

