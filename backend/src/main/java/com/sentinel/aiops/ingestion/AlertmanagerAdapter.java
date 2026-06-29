package com.sentinel.aiops.ingestion;

import com.sentinel.aiops.domain.enums.Severity;
import com.sentinel.aiops.dto.CreateIncidentRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * <b>Adapter pattern.</b> Adapts an external monitoring system's alert format
 * (Alertmanager) into the platform's internal {@link CreateIncidentRequest}.
 * Swapping in Datadog/Grafana/PagerDuty later means writing another adapter —
 * the rest of the pipeline is untouched.
 */
@Component
public class AlertmanagerAdapter {

    public CreateIncidentRequest toCreateRequest(AlertPayload.Alert alert) {
        Map<String, String> labels = alert.labels() == null ? Map.of() : alert.labels();
        Map<String, String> ann = alert.annotations() == null ? Map.of() : alert.annotations();

        String alertName = labels.getOrDefault("alertname", "Unnamed alert");
        String service = labels.getOrDefault("service",
                labels.getOrDefault("job", "unknown-service"));
        String summary = ann.getOrDefault("summary", alertName);
        String description = ann.getOrDefault("description", "");

        String rawSignal = "alertmanager: " + alertName
                + " | labels=" + labels + " | " + description;

        return new CreateIncidentRequest(
                summary,
                description,
                service,
                mapSeverity(labels.get("severity")), // null -> AI classifies
                rawSignal);
    }

    private Severity mapSeverity(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) {
            case "critical", "page" -> Severity.SEV1;
            case "warning", "high" -> Severity.SEV2;
            case "info", "low" -> Severity.SEV3;
            default -> null; // let the AI triage decide
        };
    }
}
