package com.trackableagents.controlplane.api;

public record RepoRefRequest(
    String owner,
    String repo,
    String branch,
    String commit
) {
}

