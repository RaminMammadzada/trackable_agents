#!/usr/bin/env python3
import argparse
import hashlib
import json
import os
import signal
import subprocess
import sys
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import URLError
from urllib.request import Request, urlopen


def now_iso():
    return datetime.now(timezone.utc).isoformat()


def run_git(repo, args):
    result = subprocess.run(
        ["git", *args],
        cwd=repo,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        return None
    return result.stdout.strip()


def git_remote_owner_repo(repo):
    remote = run_git(repo, ["config", "--get", "remote.origin.url"])
    if not remote:
        return None, Path(repo).name
    normalized = remote.removesuffix(".git")
    if normalized.startswith("git@github.com:"):
        owner_repo = normalized.split(":", 1)[1]
    elif "github.com/" in normalized:
        owner_repo = normalized.split("github.com/", 1)[1]
    else:
        return None, Path(repo).name
    if "/" not in owner_repo:
        return None, Path(repo).name
    return owner_repo.split("/", 1)


def list_workspace_files(repo):
    root = Path(repo)
    ignored_dirs = {".git", ".codex", ".claude", "__pycache__", "node_modules", ".venv", "venv", ".idea", ".vscode"}
    ignored_suffixes = {".pyc", ".pyo", ".DS_Store"}
    files = {}
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        if any(part in ignored_dirs for part in path.parts):
            continue
        if path.name in ignored_dirs:
            continue
        if path.suffix in ignored_suffixes:
            continue
        relative = path.relative_to(root).as_posix()
        stat = path.stat()
        files[relative] = {
            "mtimeNs": stat.st_mtime_ns,
            "size": stat.st_size,
        }
    return files


def workspace_change_summary(previous_files, current_files):
    previous_keys = set(previous_files.keys())
    current_keys = set(current_files.keys())
    added = sorted(current_keys - previous_keys)
    removed = sorted(previous_keys - current_keys)
    modified = sorted(
        path for path in (previous_keys & current_keys)
        if previous_files[path] != current_files[path]
    )
    return added, removed, modified


def has_git_repo(repo):
    return run_git(repo, ["rev-parse", "--show-toplevel"]) is not None


def status_snapshot(repo):
    if has_git_repo(repo):
        branch = run_git(repo, ["rev-parse", "--abbrev-ref", "HEAD"]) or "workspace"
        commit = run_git(repo, ["rev-parse", "HEAD"]) or ""
        status = run_git(repo, ["status", "--porcelain=v1"]) or ""
        changed_files = []
        for line in status.splitlines():
            if not line.strip():
                continue
            changed_files.append(line[3:])
        return {
            "mode": "git-poll",
            "branch": branch,
            "commit": commit,
            "status": status,
            "changedFiles": changed_files,
            "dirty": bool(changed_files),
            "fileMap": {},
        }

    file_map = list_workspace_files(repo)
    return {
        "mode": "filesystem-poll",
        "branch": "workspace",
        "commit": "",
        "status": hashlib.sha256(json.dumps(file_map, sort_keys=True).encode("utf-8")).hexdigest(),
        "changedFiles": sorted(file_map.keys()),
        "dirty": bool(file_map),
        "fileMap": file_map,
    }


def state_file_path(repo, source):
    digest = hashlib.sha256(f"{source}:{repo}".encode("utf-8")).hexdigest()[:16]
    root = Path.home() / ".trackable_agents"
    root.mkdir(parents=True, exist_ok=True)
    return root / f"watch-{digest}.json"


def load_state(path):
    if not path.exists():
        return {}
    return json.loads(path.read_text())


def save_state(path, state):
    path.write_text(json.dumps(state, indent=2))


def event_idempotency_key(source, repo, event_type, snapshot):
    raw = json.dumps(
        {
            "source": source,
            "repo": repo,
            "eventType": event_type,
            "branch": snapshot["branch"],
            "commit": snapshot["commit"],
            "status": snapshot["status"],
        },
        sort_keys=True,
        separators=(",", ":"),
    )
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def post_event(url, event):
    request = Request(
        url + "/api/v1/events",
        data=json.dumps(event).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urlopen(request, timeout=5) as response:
        response.read()


def build_event(source, repo, session_id, run_id, event_type, summary, payload, repo_ref):
    snapshot = payload["snapshot"]
    return {
        "eventId": str(uuid.uuid4()),
        "occurredAt": now_iso(),
        "receivedAt": now_iso(),
        "traceId": None,
        "sessionId": session_id,
        "runId": run_id,
        "taskId": None,
        "source": source,
        "agentRole": "implementation",
        "eventType": event_type,
        "summary": summary,
        "payload": payload,
        "artifactRefs": [],
        "idempotencyKey": event_idempotency_key(source, str(repo), event_type, snapshot),
        "repoRef": repo_ref,
    }


def main():
    parser = argparse.ArgumentParser(description="Watch an external workspace and emit activity to Trackable Agents")
    parser.add_argument("--repo", required=True, help="Absolute path to the workspace to watch")
    parser.add_argument("--source", default="copilot", help="Source label to emit, e.g. copilot")
    parser.add_argument("--interval", type=int, default=5, help="Polling interval in seconds")
    parser.add_argument("--control-plane-url", default=os.getenv("AGENT_CONTROL_PLANE_URL", "http://localhost:8080"))
    args = parser.parse_args()

    repo = str(Path(args.repo).resolve())
    if not Path(repo).is_dir():
        print(f"watch-repo-activity: {repo} is not a directory", file=sys.stderr)
        return 1

    watcher_state_file = state_file_path(repo, args.source)
    state = load_state(watcher_state_file)

    owner, repo_name = git_remote_owner_repo(repo)
    watcher_id = hashlib.sha256(f"{args.source}:{repo}".encode("utf-8")).hexdigest()[:12]
    session_id = f"watch:{args.source}:{watcher_id}"

    stop = False

    def handle_signal(signum, frame):
        nonlocal stop
        stop = True

    signal.signal(signal.SIGTERM, handle_signal)
    signal.signal(signal.SIGINT, handle_signal)

    initial = status_snapshot(repo)
    current_branch = initial["branch"]
    run_id = f"watch:{args.source}:{watcher_id}:{current_branch}"
    repo_ref = {
        "owner": owner,
        "repo": repo_name,
        "branch": initial["branch"],
        "commit": initial["commit"],
    }

    if not state.get("bootstrapped"):
        event = build_event(
            args.source,
            repo,
            session_id,
            run_id,
            "session.started",
            f"Watching external repo {repo_name} for {args.source} activity",
            {
                "watcher": {"repoPath": repo, "mode": initial["mode"], "intervalSeconds": args.interval},
                "snapshot": initial,
            },
            repo_ref,
        )
        try:
            post_event(args.control_plane_url, event)
        except (OSError, URLError, TimeoutError) as exc:
            print(f"watch-repo-activity warning: failed to emit session start: {exc}", file=sys.stderr)

        if initial["changedFiles"]:
            initial_change_event = build_event(
                args.source,
                repo,
                session_id,
                run_id,
                "file.changed",
                f"Observed {len(initial['changedFiles'])} existing file(s) in {repo_name}",
                {
                    "watcher": {"repoPath": repo, "mode": initial["mode"], "intervalSeconds": args.interval},
                    "snapshot": initial,
                },
                repo_ref,
            )
            try:
                post_event(args.control_plane_url, initial_change_event)
            except (OSError, URLError, TimeoutError) as exc:
                print(f"watch-repo-activity warning: failed to emit initial snapshot: {exc}", file=sys.stderr)

    state.update(
        {
            "bootstrapped": True,
            "mode": initial["mode"],
            "branch": initial["branch"],
            "commit": initial["commit"],
            "status": initial["status"],
            "dirty": initial["dirty"],
            "fileMap": initial.get("fileMap", {}),
        }
    )
    save_state(watcher_state_file, state)

    while not stop:
        time.sleep(args.interval)
        snapshot = status_snapshot(repo)
        repo_ref = {
            "owner": owner,
            "repo": repo_name,
            "branch": snapshot["branch"],
            "commit": snapshot["commit"],
        }
        run_id = f"watch:{args.source}:{watcher_id}:{snapshot['branch']}"
        previous_branch = state.get("branch")
        previous_commit = state.get("commit")
        previous_status = state.get("status")
        previous_dirty = state.get("dirty", False)

        if snapshot["branch"] != previous_branch:
            event_type = "session.started"
            summary = f"Observed branch switch to {snapshot['branch']} in {repo_name}"
        elif snapshot["mode"] == "filesystem-poll" and snapshot["status"] != previous_status:
            previous_files = state.get("fileMap", {})
            added, removed, modified = workspace_change_summary(previous_files, snapshot["fileMap"])
            snapshot["changedFiles"] = added + modified + removed
            event_type = "file.changed"
            summary = (
                f"Observed workspace changes in {repo_name}: "
                f"{len(added)} added, {len(modified)} modified, {len(removed)} removed"
            )
        elif snapshot["dirty"] and snapshot["status"] != previous_status:
            event_type = "file.changed"
            summary = f"Observed {len(snapshot['changedFiles'])} changed file(s) in {repo_name}"
        elif previous_dirty and not snapshot["dirty"]:
            event_type = "run.completed"
            summary = f"Repo {repo_name} returned clean on {snapshot['branch']}"
        elif snapshot["commit"] != previous_commit:
            event_type = "run.completed"
            summary = f"Observed commit advance in {repo_name} on {snapshot['branch']}"
        else:
            state.update(
                {
                    "mode": snapshot["mode"],
                    "branch": snapshot["branch"],
                    "commit": snapshot["commit"],
                    "status": snapshot["status"],
                    "dirty": snapshot["dirty"],
                    "fileMap": snapshot.get("fileMap", {}),
                }
            )
            save_state(watcher_state_file, state)
            continue

        event = build_event(
            args.source,
            repo,
            session_id,
            run_id,
            event_type,
            summary,
            {
                "watcher": {"repoPath": repo, "mode": snapshot["mode"], "intervalSeconds": args.interval},
                "snapshot": snapshot,
            },
            repo_ref,
        )
        try:
            post_event(args.control_plane_url, event)
        except (OSError, URLError, TimeoutError) as exc:
            print(f"watch-repo-activity warning: failed to emit event: {exc}", file=sys.stderr)

        state.update(
            {
                "mode": snapshot["mode"],
                "branch": snapshot["branch"],
                "commit": snapshot["commit"],
                "status": snapshot["status"],
                "dirty": snapshot["dirty"],
                "fileMap": snapshot.get("fileMap", {}),
            }
        )
        save_state(watcher_state_file, state)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
