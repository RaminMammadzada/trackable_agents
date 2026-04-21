#!/usr/bin/env python3
import json
import os
import sys
import uuid
from datetime import datetime, timezone
from urllib.request import Request, urlopen


def now_iso():
    return datetime.now(timezone.utc).isoformat()


def main():
    payload = json.load(sys.stdin) if not sys.stdin.isatty() else {}
    event = {
        "eventId": str(uuid.uuid4()),
        "occurredAt": now_iso(),
        "receivedAt": now_iso(),
        "traceId": payload.get("traceId"),
        "sessionId": os.getenv("CODEX_SESSION_ID"),
        "runId": os.getenv("CODEX_RUN_ID", os.getenv("AGENT_RUN_ID")),
        "taskId": payload.get("taskId"),
        "source": "codex",
        "agentRole": os.getenv("AGENT_ROLE", "implementation"),
        "eventType": payload.get("eventType", "prompt.submitted"),
        "summary": payload.get("summary", "Codex hook event"),
        "payload": payload,
        "artifactRefs": payload.get("artifactRefs", []),
        "idempotencyKey": payload.get("idempotencyKey", f"codex:{uuid.uuid4()}")
    }

    body = json.dumps(event).encode("utf-8")
    url = os.getenv("AGENT_CONTROL_PLANE_URL", "http://localhost:8080") + "/api/v1/events"
    request = Request(url, data=body, headers={"Content-Type": "application/json"}, method="POST")
    with urlopen(request) as response:
        sys.stdout.write(response.read().decode("utf-8"))


if __name__ == "__main__":
    main()

