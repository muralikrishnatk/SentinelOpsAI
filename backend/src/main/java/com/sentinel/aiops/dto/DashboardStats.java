package com.sentinel.aiops.dto;

import java.util.Map;

/** Aggregated metrics for the dashboard view. */
public record DashboardStats(
        long total,
        long open,
        long resolved,
        Map<String, Long> bySeverity,
        Map<String, Long> byStatus,
        double meanTimeToResolveMinutes
) {}
