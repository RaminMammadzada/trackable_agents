package com.trackableagents.controlplane.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.trackableagents.controlplane.api.AgentEventRequest;
import com.trackableagents.controlplane.api.IngestEventResponse;
import com.trackableagents.controlplane.service.EventIngestService;
import com.trackableagents.controlplane.web.github.GithubWebhookMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class IngestController {

    private final EventIngestService eventIngestService;
    private final GithubWebhookMapper githubWebhookMapper;

    public IngestController(EventIngestService eventIngestService, GithubWebhookMapper githubWebhookMapper) {
        this.eventIngestService = eventIngestService;
        this.githubWebhookMapper = githubWebhookMapper;
    }

    @PostMapping("/events")
    public IngestEventResponse ingestEvent(@Valid @RequestBody AgentEventRequest request) {
        return eventIngestService.ingest(request);
    }

    @PostMapping("/github/webhooks")
    public IngestEventResponse ingestGithubWebhook(
        @RequestHeader(value = "X-GitHub-Event", required = false) String githubEvent,
        @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
        @RequestBody JsonNode payload,
        HttpServletRequest request
    ) {
        AgentEventRequest mapped = githubWebhookMapper.map(githubEvent, userAgent, payload, request);
        return eventIngestService.ingest(mapped);
    }
}

