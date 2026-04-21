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

## GitHub bootstrap

Initialize and publish the repo after reviewing the generated files:

```bash
git init -b main
git add .
git commit -m "Initial passive observability platform"
gh repo create trackable_agents --public --source=. --remote=origin --push
```

