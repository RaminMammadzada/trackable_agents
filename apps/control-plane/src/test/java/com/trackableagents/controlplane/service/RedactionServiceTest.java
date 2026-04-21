package com.trackableagents.controlplane.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.trackableagents.controlplane.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedactionServiceTest {

    private final RedactionService redactionService = new RedactionService(new AppProperties());

    @Test
    void redactsSensitiveKeysAndTokenPatterns() {
        var payload = JsonNodeFactory.instance.objectNode()
            .put("token", "ghp_abcdef123456")
            .put("message", "Bearer abc.def");

        var redacted = redactionService.redact(payload);

        assertThat(redacted.get("token").asText()).isEqualTo("[REDACTED]");
        assertThat(redacted.get("message").asText()).isEqualTo("[REDACTED]");
        assertThat(redactionService.redactText("secret sk-test-123456789012")).doesNotContain("sk-test");
    }
}

