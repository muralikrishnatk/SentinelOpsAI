package com.sentinel.aiops.service.notification;

import com.sentinel.aiops.domain.Incident;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>Adapter</b> for a generic outbound webhook (Teams, Opsgenie, custom SIEM, etc.).
 * Live when a URL is configured; otherwise logs (simulated).
 */
@Component
@Slf4j
public class WebhookNotifier implements Notifier {

    private final String url;
    private final RestClient http = RestClient.create();

    public WebhookNotifier(@Value("${aiops.integrations.webhook.url:}") String url) {
        this.url = url;
    }

    @Override public String channel() { return "webhook"; }
    @Override public boolean live() { return url != null && !url.isBlank(); }
    @Override public String detail() { return live() ? url : "log only (set aiops.integrations.webhook.url)"; }

    @Override
    public void send(Incident i, String message) {
        if (!live()) { log.info("[WEBHOOK] {}", message); return; }
        Map<String, Object> body = new HashMap<>();
        body.put("incidentId", i.getId());
        body.put("severity", i.getSeverity() == null ? null : i.getSeverity().name());
        body.put("service", i.getAffectedService());
        body.put("title", i.getTitle());
        body.put("message", message);
        http.post().uri(url).body(body).retrieve().toBodilessEntity();
        log.info("[WEBHOOK] delivered incident #{}", i.getId());
    }
}
