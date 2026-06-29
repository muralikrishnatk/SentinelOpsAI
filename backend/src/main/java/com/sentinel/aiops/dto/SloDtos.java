package com.sentinel.aiops.dto;

import com.sentinel.aiops.domain.enums.SliType;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class SloDtos {

    public record SloRequest(
            @NotBlank String service,
            @NotBlank String name,
            SliType sliType,
            double objectivePercent,
            Integer windowDays,
            Integer latencyThresholdMs) {}

    public record BurnWindow(String label, double burnRate, boolean breaching) {}

    /** Live SLO status computed from the active SLI source. */
    public record SloView(
            Long id,
            String service,
            String name,
            SliType sliType,
            double objectivePercent,
            int windowDays,
            Integer latencyThresholdMs,
            double currentSliPercent,      // e.g. 99.95
            double budgetHeadroomPercent,  // (1 - burn_long)*100; negative = over budget
            String alertTier,              // FAST | SLOW | TICKET | NONE
            List<BurnWindow> windows,
            String sliSource,              // prometheus(...) | in-memory
            boolean faulted) {}

    public record AlertView(
            Long id, String service, String sloName, String tier,
            double burnRateShort, double burnRateLong, Long incidentId, Instant firedAt) {}
}
