#!/usr/bin/env bash
set -euo pipefail

CONTROL_PLANE_URL="${AGENT_CONTROL_PLANE_URL:-http://localhost:8080}"

curl --fail --silent --show-error \
  -X POST \
  "${CONTROL_PLANE_URL}/api/v1/admin/runtime/reset"
echo
