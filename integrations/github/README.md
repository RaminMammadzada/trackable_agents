# GitHub / Copilot Integration

V1 ingests GitHub-native activity through `POST /api/v1/github/webhooks`.

Recommended webhook events:

- `pull_request`
- `pull_request_review`
- `pull_request_review_comment`
- `check_run`
- `check_suite`
- `workflow_run`

Each webhook is normalized into the canonical event schema with source `github`. Copilot-originated actions are reconstructed through PR/check/review activity instead of local hooks.

