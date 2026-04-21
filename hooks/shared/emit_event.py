#!/usr/bin/env python3
import hashlib
import json
import os
import subprocess
import sys
import uuid
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import URLError
from urllib.request import Request, urlopen


EVENT_TYPE_MAP = {
    "SessionStart": "session.started",
    "SessionEnd": "session.ended",
    "UserPromptSubmit": "prompt.submitted",
    "PreToolUse": "tool.pre_call",
    "PostToolUse": "tool.post_call",
    "PostToolUseFailure": "failure.recorded",
    "PermissionRequest": "approval.requested",
    "Stop": "run.completed",
}


def now_iso():
    return datetime.now(timezone.utc).isoformat()


def read_payload():
    if sys.stdin.isatty():
        return {}
    raw = sys.stdin.read()
    if not raw.strip():
        return {}
    return json.loads(raw)


def first_non_empty(*values):
    for value in values:
        if isinstance(value, str) and value.strip():
            return value
        if value:
            return value
    return None


def infer_hook_event_name(payload):
    return first_non_empty(
        os.getenv("TRACKABLE_HOOK_EVENT"),
        payload.get("hook_event_name"),
        payload.get("hookEventName"),
        payload.get("event_name"),
        payload.get("eventName"),
    )


def infer_run_id(payload):
    source = os.getenv("TRACKABLE_SOURCE", "unknown")
    explicit = first_non_empty(
        os.getenv("TRACKABLE_RUN_ID"),
        os.getenv("AGENT_RUN_ID"),
        os.getenv("CLAUDE_RUN_ID"),
        os.getenv("CODEX_RUN_ID"),
        payload.get("run_id"),
        payload.get("runId"),
    )
    session_id = infer_session_id(payload)
    task_id = first_non_empty(payload.get("task_id"), payload.get("taskId"))
    turn_id = first_non_empty(payload.get("turn_id"), payload.get("turnId"))

    if explicit:
        return explicit if str(explicit).startswith(f"{source}:") else f"{source}:{explicit}"
    if session_id and turn_id:
        return f"{source}:{session_id}:{turn_id}"
    if session_id and task_id:
        return f"{source}:{session_id}:{task_id}"
    if session_id:
        return f"{source}:{session_id}"
    return f"{source}:unscoped"


def infer_session_id(payload):
    return first_non_empty(
        os.getenv("TRACKABLE_SESSION_ID"),
        os.getenv("CLAUDE_SESSION_ID"),
        os.getenv("CODEX_SESSION_ID"),
        payload.get("session_id"),
        payload.get("sessionId"),
    )


def infer_task_id(payload):
    return first_non_empty(
        payload.get("task_id"),
        payload.get("taskId"),
        payload.get("subagent_id"),
        payload.get("subagentId"),
        payload.get("tool_use_id"),
        payload.get("toolUseId"),
    )


def infer_role(payload):
    tool_name = first_non_empty(payload.get("tool_name"), payload.get("toolName"), "")
    if tool_name in {"Read", "Glob", "Grep", "LS"}:
        return "planner"
    if tool_name in {"Bash", "Edit", "Write", "MultiEdit"}:
        return "implementation"
    return os.getenv("TRACKABLE_AGENT_ROLE", "implementation")


def summarize(payload, hook_event_name, event_type):
    if event_type in {"tool.pre_call", "tool.post_call", "failure.recorded", "approval.requested"}:
        tool_name = first_non_empty(payload.get("tool_name"), payload.get("toolName"), "unknown-tool")
        tool_input = payload.get("tool_input") or payload.get("toolInput") or {}
        command = first_non_empty(
            tool_input.get("command") if isinstance(tool_input, dict) else None,
            tool_input.get("file_path") if isinstance(tool_input, dict) else None,
            tool_input.get("path") if isinstance(tool_input, dict) else None,
        )
        if command:
            return f"{hook_event_name}: {tool_name} {command}"
        return f"{hook_event_name}: {tool_name}"

    prompt = first_non_empty(payload.get("prompt"), payload.get("user_prompt"), payload.get("text"))
    if event_type == "prompt.submitted" and prompt:
        compact = " ".join(str(prompt).split())
        return f"Prompt submitted: {compact[:180]}"

    return first_non_empty(
        payload.get("summary"),
        payload.get("message"),
        f"{hook_event_name or 'unknown'} from {os.getenv('TRACKABLE_SOURCE', 'unknown')}",
    )


def git_metadata():
    root = run_git(["rev-parse", "--show-toplevel"])
    if not root:
        return None

    branch = run_git(["rev-parse", "--abbrev-ref", "HEAD"], cwd=root)
    commit = run_git(["rev-parse", "HEAD"], cwd=root)
    remote = run_git(["config", "--get", "remote.origin.url"], cwd=root)

    owner = None
    repo = None
    if remote:
        normalized = remote.removesuffix(".git")
        if normalized.startswith("git@github.com:"):
            owner_repo = normalized.split(":", 1)[1]
        elif "github.com/" in normalized:
            owner_repo = normalized.split("github.com/", 1)[1]
        else:
            owner_repo = None
        if owner_repo and "/" in owner_repo:
            owner, repo = owner_repo.split("/", 1)

    return {
        "owner": owner,
        "repo": repo,
        "branch": branch,
        "commit": commit,
        "root": root,
    }


def run_git(args, cwd=None):
    try:
        result = subprocess.run(
            ["git", *args],
            cwd=cwd,
            capture_output=True,
            text=True,
            check=True,
        )
        return result.stdout.strip()
    except (subprocess.CalledProcessError, FileNotFoundError):
        return None


def build_idempotency_key(source, hook_event_name, payload):
    stable_payload = json.dumps(payload, sort_keys=True, separators=(",", ":"))
    digest = hashlib.sha256(stable_payload.encode("utf-8")).hexdigest()[:20]
    session_id = first_non_empty(payload.get("session_id"), payload.get("sessionId"), "no-session")
    turn_id = first_non_empty(payload.get("turn_id"), payload.get("turnId"), "no-turn")
    return f"{source}:{hook_event_name}:{session_id}:{turn_id}:{digest}"


def send_event(event):
    body = json.dumps(event).encode("utf-8")
    url = os.getenv("AGENT_CONTROL_PLANE_URL", "http://localhost:8080") + "/api/v1/events"
    request = Request(url, data=body, headers={"Content-Type": "application/json"}, method="POST")
    with urlopen(request, timeout=5) as response:
        response.read()


def main():
    payload = read_payload()
    source = os.getenv("TRACKABLE_SOURCE", "unknown")
    hook_event_name = infer_hook_event_name(payload)
    event_type = EVENT_TYPE_MAP.get(hook_event_name)
    if not event_type:
        return

    repo = git_metadata() or {}
    event = {
        "eventId": str(uuid.uuid4()),
        "occurredAt": now_iso(),
        "receivedAt": now_iso(),
        "traceId": first_non_empty(payload.get("trace_id"), payload.get("traceId")),
        "sessionId": infer_session_id(payload),
        "runId": infer_run_id(payload),
        "taskId": infer_task_id(payload),
        "source": source,
        "agentRole": infer_role(payload),
        "eventType": event_type,
        "summary": summarize(payload, hook_event_name, event_type),
        "payload": payload,
        "artifactRefs": [],
        "idempotencyKey": build_idempotency_key(source, hook_event_name, payload),
        "repoRef": {
            "owner": repo.get("owner"),
            "repo": repo.get("repo"),
            "branch": repo.get("branch"),
            "commit": repo.get("commit"),
        },
    }

    try:
        send_event(event)
    except (OSError, URLError, TimeoutError) as exception:
        print(f"trackable_agents hook warning: failed to emit {hook_event_name}: {exception}", file=sys.stderr)


if __name__ == "__main__":
    main()
