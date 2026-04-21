package com.trackableagents.controlplane.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class JsonMapper {

    private final ObjectMapper objectMapper;

    public JsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node == null ? JsonNodeFactory.instance.objectNode() : node);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize JSON payload", exception);
        }
    }

    public JsonNode read(String value) {
        try {
            return objectMapper.readTree(value == null || value.isBlank() ? "{}" : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to read stored JSON payload", exception);
        }
    }

    public ObjectNode objectNode() {
        return objectMapper.createObjectNode();
    }

    public ArrayNode arrayNode() {
        return objectMapper.createArrayNode();
    }

    public JsonNode valueToTree(Object value) {
        return objectMapper.valueToTree(value);
    }
}
