package com.trackableagents.controlplane.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.trackableagents.controlplane.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RedactionService {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "token", "api_key", "apikey", "authorization", "password", "secret", "github_token"
    );

    private static final Pattern SECRET_PATTERN = Pattern.compile(
        "(gh[pousr]_[A-Za-z0-9_]+|sk-[A-Za-z0-9_-]{12,}|Bearer\\s+[A-Za-z0-9._-]+)",
        Pattern.CASE_INSENSITIVE
    );

    private final AppProperties properties;

    public RedactionService(AppProperties properties) {
        this.properties = properties;
    }

    public JsonNode redact(JsonNode input) {
        if (!properties.getRedaction().isEnabled() || input == null || input.isNull()) {
            return input == null ? JsonNodeFactory.instance.objectNode() : input;
        }
        if (input.isObject()) {
            ObjectNode copy = JsonNodeFactory.instance.objectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = input.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_KEYS.contains(field.getKey().toLowerCase())) {
                    copy.put(field.getKey(), "[REDACTED]");
                } else {
                    copy.set(field.getKey(), redact(field.getValue()));
                }
            }
            return copy;
        }
        if (input.isArray()) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            input.forEach(item -> array.add(redact(item)));
            return array;
        }
        if (input.isTextual()) {
            return TextNode.valueOf(redactText(input.asText()));
        }
        return input;
    }

    public String redactText(String text) {
        if (!properties.getRedaction().isEnabled() || text == null) {
            return text;
        }
        return SECRET_PATTERN.matcher(text).replaceAll("[REDACTED]");
    }
}

