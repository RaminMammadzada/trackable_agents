package com.trackableagents.controlplane.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.trackableagents.controlplane.model.EventType;
import com.trackableagents.controlplane.model.RiskLevel;
import org.springframework.stereotype.Service;

@Service
public class RiskClassifier {

    public RiskLevel classify(String summary, JsonNode payload, EventType eventType) {
        String haystack = (summary == null ? "" : summary) + " " + (payload == null ? "" : payload.toString());
        String normalized = haystack.toLowerCase();

        RiskLevel level = switch (eventType) {
            case SESSION_STARTED, SESSION_ENDED, PROMPT_SUBMITTED -> RiskLevel.R0;
            case TOOL_PRE_CALL, TOOL_POST_CALL, TEST_STARTED, TEST_FINISHED -> RiskLevel.R1;
            case FILE_CHANGED, PR_CREATED, RUN_COMPLETED, REVIEW_COMPLETED -> RiskLevel.R2;
            case FAILURE_RECORDED, APPROVAL_REQUESTED, APPROVAL_RESOLVED, LESSON_PROPOSED -> RiskLevel.R2;
            case RISK_DETECTED -> RiskLevel.R3;
        };

        if (containsAny(normalized, "auth", "billing", "payment", "migration", "dependency", "schema")) {
            level = RiskLevel.max(level, RiskLevel.R3);
        }
        if (containsAny(normalized, "secret", "token", "terraform apply", "kubectl", "aws iam", ".env", "prod")) {
            level = RiskLevel.max(level, RiskLevel.R4);
        }
        if (containsAny(normalized, "exfiltrate", "disable security", "bypass test", "delete backups")) {
            level = RiskLevel.R5;
        }

        return level;
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}

