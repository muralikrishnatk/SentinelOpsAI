package com.sentinel.aiops.ingestion;

import com.sentinel.aiops.dto.IncidentResponse;
import com.sentinel.aiops.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ingestion endpoint for monitoring systems. Each firing alert is adapted into
 * an incident and pushed through the full AI triage pipeline automatically —
 * this is the "real-world integration" path (no human in the loop).
 */
@RestController
@RequestMapping("/api/ingest")
@Tag(name = "Ingestion", description = "Monitoring webhook intake (Alertmanager-compatible)")
@Slf4j
public class AlertWebhookController {

    private final AlertmanagerAdapter adapter;
    private final IncidentService incidents;

    public AlertWebhookController(AlertmanagerAdapter adapter, IncidentService incidents) {
        this.adapter = adapter;
        this.incidents = incidents;
    }

    @PostMapping("/alertmanager")
    @Operation(summary = "Receive an Alertmanager webhook; creates triaged incidents")
    public Map<String, Object> ingest(@RequestBody AlertPayload payload) {
        List<Long> created = new ArrayList<>();
        if (payload.alerts() != null) {
            for (AlertPayload.Alert alert : payload.alerts()) {
                if (!"firing".equalsIgnoreCase(alert.status())) continue;
                IncidentResponse r = incidents.createFromAlert(adapter.toCreateRequest(alert));
                created.add(r.id());
            }
        }
        log.info("Ingested {} firing alert(s) -> incidents {}", created.size(), created);
        return Map.of("createdIncidentIds", created, "count", created.size());
    }
}
