package com.sentinel.aiops.dto;

import com.sentinel.aiops.domain.enums.ServiceTier;
import jakarta.validation.constraints.NotBlank;

public class ServiceDtos {

    public record ServiceRequest(
            @NotBlank String name,
            String description,
            String ownerTeam,
            ServiceTier tier,
            String runbookUrl) {}

    /** health: HEALTHY | DEGRADED | CRITICAL, derived from open incidents. */
    public record ServiceView(
            Long id,
            String name,
            String description,
            String ownerTeam,
            ServiceTier tier,
            String runbookUrl,
            String health,
            long openIncidents) {}
}
