package com.sentinel.aiops.dto;

import com.sentinel.aiops.domain.enums.Severity;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound DTO (Data Transfer Object pattern). Decouples the wire format
 * from the persistence entity and carries validation constraints.
 * If severity is null, the AI triage pipeline will classify it.
 */
public record CreateIncidentRequest(
        @NotBlank String title,
        String description,
        @NotBlank String affectedService,
        Severity severity,        // optional - AI infers when null
        String rawSignal          // raw log/alert payload
) {}
