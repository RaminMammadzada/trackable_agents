# Trackable Agents

Trackable Agents is a passive observability platform for agent-assisted development sessions. It captures normalized events from Codex, Claude Code, and GitHub/Copilot activity, reconstructs runs, classifies risk, stores failures and lessons, and exposes a minimal dashboard plus API.

## V1 scope

- Passive monitoring only
- Single-admin operation
- Docker-hosted local/team deployment
- PostgreSQL-backed append-only event ledger
- Human-approved lesson promotion

## Repository layout

- `apps/control-plane`: Spring Boot service for ingestion, projections, policy, learning, and UI APIs
- `apps/web`: minimal dashboard assets and notes for the V1 UI
- `hooks/codex`, `hooks/claude`: lightweight emitters for local agent hooks
- `integrations/github`: GitHub/Copilot webhook integration notes
- `ops/docker`: Docker Compose, OpenTelemetry Collector, Grafana, and provisioning
- `docs/architecture`: ADRs, event schema, risk model, and adapter contracts

## Quick start

1. Start infrastructure:

```bash
docker compose up -d postgres otel-collector grafana tempo
```

2. Run the control plane:

```bash
cd apps/control-plane
./mvnw spring-boot:run
```

3. Open:

- App UI: `http://localhost:8080`
- Grafana: `http://localhost:3000`
- Health: `http://localhost:8080/actuator/health`

## Local tracking hooks

This repo now includes repo-local hook configs for both tools:

- Codex: `.codex/hooks.json`
- Claude Code: `.claude/settings.json`

They send lifecycle events to the local control plane at `http://localhost:8080/api/v1/events`.

Notes:

- Run Codex or Claude Code from inside this repository so the repo-local hook config is discovered.
- Keep the control plane running while you work.
- The hook emitter is intentionally non-blocking. If the tracking service is down, your agent session continues and the hook only writes a warning to stderr.
- Claude Code project hooks are committed in `.claude/settings.json`, which follows the current Claude Code project-hook location.
- Codex project hooks are committed in `.codex/hooks.json`, which follows the current Codex repo-local hook location.

## GitHub bootstrap

Initialize and publish the repo after reviewing the generated files:

```bash
git init -b main
git add .
git commit -m "Initial passive observability platform"
gh repo create trackable_agents --public --source=. --remote=origin --push
```
