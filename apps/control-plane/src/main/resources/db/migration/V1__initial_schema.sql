CREATE TABLE agent_events (
    event_id VARCHAR(64) PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    trace_id VARCHAR(128),
    session_id VARCHAR(128),
    run_id VARCHAR(128),
    task_id VARCHAR(128),
    source VARCHAR(32) NOT NULL,
    agent_role VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    risk_level VARCHAR(8) NOT NULL,
    repo_owner VARCHAR(255),
    repo_name VARCHAR(255),
    repo_branch VARCHAR(255),
    repo_commit VARCHAR(255),
    summary VARCHAR(500) NOT NULL,
    payload_json TEXT NOT NULL,
    artifact_refs_json TEXT NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE
);

CREATE INDEX idx_agent_events_run_id ON agent_events (run_id);
CREATE INDEX idx_agent_events_session_id ON agent_events (session_id);
CREATE INDEX idx_agent_events_occurred_at ON agent_events (occurred_at);

CREATE TABLE artifact_records (
    artifact_id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    run_id VARCHAR(128),
    label VARCHAR(255) NOT NULL,
    storage_path TEXT,
    mime_type VARCHAR(255),
    checksum VARCHAR(255),
    size_bytes BIGINT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE run_projections (
    run_id VARCHAR(128) PRIMARY KEY,
    session_id VARCHAR(128),
    status VARCHAR(64) NOT NULL,
    source VARCHAR(32),
    risk_level VARCHAR(8) NOT NULL,
    started_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    event_count BIGINT NOT NULL,
    failure_count BIGINT NOT NULL,
    lesson_count BIGINT NOT NULL,
    last_event_type VARCHAR(64),
    last_summary VARCHAR(500),
    title VARCHAR(500)
);

CREATE TABLE session_projections (
    session_id VARCHAR(128) PRIMARY KEY,
    status VARCHAR(64) NOT NULL,
    started_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    run_count BIGINT NOT NULL,
    risk_level VARCHAR(8) NOT NULL
);

CREATE TABLE task_projections (
    task_id VARCHAR(128) PRIMARY KEY,
    run_id VARCHAR(128),
    status VARCHAR(64) NOT NULL,
    risk_level VARCHAR(8) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_event_type VARCHAR(64),
    last_summary VARCHAR(500)
);

CREATE TABLE failure_records (
    failure_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(128) NOT NULL,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    summary VARCHAR(500) NOT NULL,
    details_json TEXT NOT NULL,
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE lesson_proposals (
    lesson_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(128) NOT NULL,
    failure_id VARCHAR(64),
    title VARCHAR(300) NOT NULL,
    summary VARCHAR(1200) NOT NULL,
    details_json TEXT NOT NULL,
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    decision_note VARCHAR(1200)
);

