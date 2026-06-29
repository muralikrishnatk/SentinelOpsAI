package com.sentinel.aiops.integration.health;

/** One row on the integrations page. */
public record IntegrationStatus(
        String name, String category, IntegrationState state, String detail, long latencyMs) {}
