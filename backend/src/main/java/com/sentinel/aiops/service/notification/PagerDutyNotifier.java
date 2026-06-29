package com.sentinel.aiops.service.notification;

import com.sentinel.aiops.domain.Incident;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * <b>Adapter</b> over the PagerDuty Events API v2. Live when a routing key is
 * configured; otherwise logs (simulated).
 */
@Component
@Slf4j
public class PagerDutyNotifier implements Notifier {

    private static final String EVENTS_URL = "https://events.pagerduty.com/v2/enqueue";
    private final String routingKey;
    private final RestClient http = RestClient.create();

    public PagerDutyNotifier(@Value("${aiops.integrations.pagerduty.routing-key:}") String routingKey) {
        this.routingKey = routingKey;
    }

    @Override public String channel() { return "pagerduty"; }
    @Override public boolean live() { return routingKey != null && !routingKey.isBlank(); }
    @Override public String detail() { return live() ? "Events API v2" : "log only (set aiops.integrations.pagerduty.routing-key)"; }

    @Override
    public void send(Incident i, String message) {
        if (!live()) { log.info("[PAGERDUTY] PAGE {} | {}", i.getAssignee(), message); return; }
        http.post().uri(EVENTS_URL)
                .body(Map.of(
                        "routing_key", routingKey,
                        "event_action", "trigger",
                        "dedup_key", "incident-" + i.getId(),
                        "payload", Map.of(
                                "summary", message,
                                "severity", i.getSeverity() == null ? "error" : i.getSeverity().name().toLowerCase(),
                                "source", i.getAffectedService())))
                .retrieve().toBodilessEntity();
        log.info("[PAGERDUTY] triggered incident #{}", i.getId());
    }
}
