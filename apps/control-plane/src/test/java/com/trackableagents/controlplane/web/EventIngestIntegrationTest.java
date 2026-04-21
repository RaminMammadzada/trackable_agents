package com.trackableagents.controlplane.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class EventIngestIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("trackable_agents_test")
        .withUsername("trackable_agents")
        .withPassword("trackable_agents");

    @DynamicPropertySource
    static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ingestsEventAndBuildsRunProjection() throws Exception {
        String payload = """
            {
              "eventId": "evt-123",
              "occurredAt": "2026-04-21T10:00:00Z",
              "receivedAt": "2026-04-21T10:00:01Z",
              "traceId": "trace-123",
              "sessionId": "session-123",
              "runId": "run-123",
              "taskId": "task-123",
              "source": "codex",
              "agentRole": "implementation",
              "eventType": "failure.recorded",
              "summary": "Dependency auth upgrade failed with token ghp_abcdef123456",
              "payload": {"tool": "npm test", "token": "ghp_abcdef123456"},
              "artifactRefs": [{"label": "test-log", "path": "logs/test.log", "mimeType": "text/plain"}],
              "idempotencyKey": "evt-123"
            }
            """;

        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.duplicate", equalTo(false)))
            .andExpect(jsonPath("$.riskLevel", equalTo("R4")));

        mockMvc.perform(get("/api/v1/runs/run-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.run.runId", equalTo("run-123")))
            .andExpect(jsonPath("$.run.failureCount", equalTo(1)))
            .andExpect(jsonPath("$.events[0].payload.token", equalTo("[REDACTED]")));
    }

    @Test
    void seedsAndResetsRuntimeData() throws Exception {
        mockMvc.perform(post("/api/v1/admin/demo/seed"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.action", equalTo("seed-demo-data")))
            .andExpect(jsonPath("$.counts.eventsCreated", equalTo(13)));

        mockMvc.perform(get("/api/v1/runs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(post("/api/v1/admin/runtime/reset"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.action", equalTo("reset-runtime-data")))
            .andExpect(jsonPath("$.counts.runsDeleted", equalTo(3)));

        mockMvc.perform(get("/api/v1/runs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
