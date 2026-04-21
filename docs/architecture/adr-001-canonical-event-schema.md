# ADR 001: Canonical Event Schema

The control plane uses one append-only event contract across Codex, Claude, GitHub, and Copilot-derived activity.

Required fields:

- `eventId`
- `occurredAt`
- `receivedAt`
- `source`
- `agentRole`
- `eventType`
- `summary`
- `idempotencyKey`

Optional correlation fields:

- `traceId`
- `sessionId`
- `runId`
- `taskId`
- `repoRef`
- `artifactRefs`

Rules:

- adapters redact sensitive values before transmission whenever possible
- the control plane redacts again before persistence
- the ledger is append-only
- projections may be rebuilt from event order at any time

