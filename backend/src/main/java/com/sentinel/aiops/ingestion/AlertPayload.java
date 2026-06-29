package com.sentinel.aiops.ingestion;

import java.util.List;
import java.util.Map;

/**
 * Minimal Alertmanager-compatible webhook payload. Real Prometheus
 * Alertmanager posts this exact shape, so this endpoint drops into a real
 * monitoring stack with no glue code.
 */
public record AlertPayload(List<Alert> alerts) {
    public record Alert(
            String status,                 // "firing" | "resolved"
            Map<String, String> labels,    // alertname, service, severity, ...
            Map<String, String> annotations // summary, description, ...
    ) {}
}
