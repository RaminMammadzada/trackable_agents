package com.trackableagents.controlplane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Storage storage = new Storage();
    private final Payload payload = new Payload();
    private final Redaction redaction = new Redaction();
    private final Github github = new Github();

    public Storage getStorage() {
        return storage;
    }

    public Payload getPayload() {
        return payload;
    }

    public Redaction getRedaction() {
        return redaction;
    }

    public Github getGithub() {
        return github;
    }

    public static class Storage {
        private String artifactRoot = "../../artifacts";

        public String getArtifactRoot() {
            return artifactRoot;
        }

        public void setArtifactRoot(String artifactRoot) {
            this.artifactRoot = artifactRoot;
        }
    }

    public static class Payload {
        private long maxSizeBytes = 32768;

        public long getMaxSizeBytes() {
            return maxSizeBytes;
        }

        public void setMaxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
        }
    }

    public static class Redaction {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Github {
        private String webhookSecret = "";

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }
}

