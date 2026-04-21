package com.trackableagents.controlplane.api;

import jakarta.validation.constraints.NotBlank;

public record ArtifactRefRequest(
    @NotBlank String label,
    String path,
    String mimeType,
    String checksum,
    Long sizeBytes
) {
}

