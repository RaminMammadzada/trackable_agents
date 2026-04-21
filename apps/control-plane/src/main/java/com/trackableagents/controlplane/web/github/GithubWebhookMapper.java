package com.trackableagents.controlplane.web.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.trackableagents.controlplane.api.AgentEventRequest;
import com.trackableagents.controlplane.api.RepoRefRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class GithubWebhookMapper {

    public AgentEventRequest map(String githubEvent, String userAgent, JsonNode payload, HttpServletRequest request) {
        JsonNode repository = payload.path("repository");
        JsonNode pullRequest = payload.path("pull_request");
        JsonNode checkRun = payload.path("check_run");
        JsonNode action = payload.path("action");

        String runId = extractRunId(payload, pullRequest);
        String summary = githubEvent + " " + (action.isMissingNode() ? "received" : action.asText("received"));
        String eventType = mapEventType(githubEvent, payload);

        String deliveryId = request.getHeader("X-GitHub-Delivery");

        return new AgentEventRequest(
            UUID.randomUUID().toString(),
            Instant.now(),
            Instant.now(),
            deliveryId,
            payload.path("workflow_run").path("id").asText(null),
            runId,
            null,
            "github",
            githubEvent != null && githubEvent.contains("review") ? "review" : "implementation",
            eventType,
            null,
            new RepoRefRequest(
                repository.path("owner").path("login").asText(null),
                repository.path("name").asText(null),
                pullRequest.path("head").path("ref").asText(repository.path("default_branch").asText(null)),
                checkRun.path("head_sha").asText(pullRequest.path("head").path("sha").asText(null))
            ),
            summary + " via " + (userAgent == null ? "unknown-agent" : userAgent),
            payload,
            List.of(),
            "github:" + (deliveryId == null ? UUID.randomUUID() : deliveryId)
        );
    }

    private String extractRunId(JsonNode payload, JsonNode pullRequest) {
        if (pullRequest.hasNonNull("head") && pullRequest.path("head").hasNonNull("ref")) {
            return "pr-" + pullRequest.path("number").asText();
        }
        if (payload.path("check_run").hasNonNull("check_suite") && payload.path("check_run").path("check_suite").hasNonNull("id")) {
            return "check-suite-" + payload.path("check_run").path("check_suite").path("id").asText();
        }
        return "github-" + UUID.randomUUID();
    }

    private String mapEventType(String githubEvent, JsonNode payload) {
        if ("pull_request".equals(githubEvent)) {
            return "pr.created";
        }
        if ("check_run".equals(githubEvent) || "check_suite".equals(githubEvent) || "workflow_run".equals(githubEvent)) {
            return payload.path("action").asText("").contains("completed") ? "test.finished" : "test.started";
        }
        if ("pull_request_review".equals(githubEvent) || "pull_request_review_comment".equals(githubEvent)) {
            return "review.completed";
        }
        return "prompt.submitted";
    }
}
