# Adapter Contracts

## Codex / Claude local emitters

- accept JSON on stdin
- enrich with environment-derived session/run metadata
- POST to `/api/v1/events`
- never assume local payloads are already safe; redaction still happens server-side

## GitHub webhook mapper

- receives raw GitHub webhook JSON
- maps known event families into canonical event types
- correlates PR/check/review activity into synthetic runs when native run IDs are unavailable

