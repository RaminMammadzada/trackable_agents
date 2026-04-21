async function fetchJson(url, options = {}) {
  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
  return response.json();
}

function setAdminStatus(message, isError = false) {
  const node = document.getElementById("adminStatus");
  node.textContent = message;
  node.className = isError ? "status error" : "status";
}

function statCard(label, value) {
  return `<div class="stat"><div class="muted">${label}</div><strong>${value}</strong></div>`;
}

function runCard(run) {
  return `
    <div class="card">
      <div><strong>${run.title || run.runId}</strong></div>
      <div class="muted">${run.runId}</div>
      <div>
        <span class="pill">${run.source || "unknown"}</span>
        <span class="pill risk-${run.riskLevel}">${run.riskLevel}</span>
        <span class="pill">${run.status}</span>
      </div>
      <p class="muted">${run.lastSummary || "No summary yet"}</p>
      <button data-run-id="${run.runId}">View Run</button>
    </div>
  `;
}

function failureCard(failure) {
  return `
    <div class="card">
      <strong>${failure.summary}</strong>
      <div class="muted">${failure.runId} · ${failure.status}</div>
    </div>
  `;
}

function lessonCard(lesson) {
  return `
    <div class="card">
      <strong>${lesson.title}</strong>
      <div class="muted">${lesson.status} · ${lesson.runId}</div>
      <p>${lesson.summary}</p>
    </div>
  `;
}

function eventCard(event) {
  return `
    <div class="card">
      <div><strong>${event.eventType}</strong></div>
      <div class="muted">${new Date(event.occurredAt).toLocaleString()} · ${event.source} · ${event.riskLevel}</div>
      <p>${event.summary}</p>
      <pre>${JSON.stringify(event.payload, null, 2)}</pre>
    </div>
  `;
}

async function loadSummary() {
  const summary = await fetchJson("/api/v1/dashboard/summary");
  document.getElementById("summary").innerHTML = [
    statCard("Runs", summary.totalRuns),
    statCard("Active", summary.activeRuns),
    statCard("Attention", summary.attentionRequiredRuns),
    statCard("Failures", summary.totalFailures),
    statCard("Pending Lessons", summary.pendingLessons),
    statCard("Codex / Claude / Copilot", `${summary.codexRuns} / ${summary.claudeRuns} / ${summary.copilotRuns}`)
  ].join("");
}

async function loadRuns() {
  const runs = await fetchJson("/api/v1/runs");
  const container = document.getElementById("runs");
  if (!runs.length) {
    container.innerHTML = `<div class="empty">No runs ingested yet.</div>`;
    return;
  }
  container.innerHTML = runs.map(runCard).join("");
  container.querySelectorAll("button[data-run-id]").forEach(button => {
    button.addEventListener("click", () => loadRunDetail(button.dataset.runId));
  });
}

async function loadRunDetail(runId) {
  const detail = await fetchJson(`/api/v1/runs/${runId}`);
  document.getElementById("selectedRun").textContent = runId;
  document.getElementById("runDetail").innerHTML = `
    <div class="card">
      <div><strong>${detail.run.title}</strong></div>
      <div class="muted">${detail.run.status} · ${detail.run.riskLevel} · ${detail.run.eventCount} events</div>
    </div>
    <div class="list">${detail.events.map(eventCard).join("")}</div>
  `;
}

async function loadFailures() {
  const failures = await fetchJson("/api/v1/failures");
  document.getElementById("failures").innerHTML = failures.length
    ? failures.map(failureCard).join("")
    : `<div class="empty">No failures recorded.</div>`;
}

async function loadLessons() {
  const lessons = await fetchJson("/api/v1/lessons");
  document.getElementById("lessons").innerHTML = lessons.length
    ? lessons.map(lessonCard).join("")
    : `<div class="empty">No lesson proposals yet.</div>`;
}

async function refreshAll() {
  await Promise.all([loadSummary(), loadRuns(), loadFailures(), loadLessons()]);
}

async function postAdminAction(url, confirmText = null) {
  if (confirmText && !window.confirm(confirmText)) {
    return;
  }
  setAdminStatus("Working...");
  try {
    const result = await fetchJson(url, { method: "POST" });
    const counts = result.counts
      ? Object.entries(result.counts).map(([key, value]) => `${key}: ${value}`).join(" · ")
      : "";
    setAdminStatus(counts ? `${result.message} ${counts}` : result.message);
    await refreshAll();
  } catch (error) {
    setAdminStatus(error.message, true);
  }
}

document.getElementById("refreshRuns").addEventListener("click", refreshAll);
document.getElementById("seedDemo").addEventListener("click", () => postAdminAction("/api/v1/admin/demo/seed"));
document.getElementById("resetRuntime").addEventListener("click", () =>
  postAdminAction("/api/v1/admin/runtime/reset", "Clear all local runtime data and demo events?")
);
refreshAll().catch(error => {
  document.getElementById("runDetail").innerHTML = `<div class="empty">${error.message}</div>`;
});
