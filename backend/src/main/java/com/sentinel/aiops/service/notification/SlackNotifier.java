package com.sentinel.aiops.service.notification;

import com.sentinel.aiops.domain.Incident;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * <b>Adapter</b> over Slack incoming webhooks. Live when a webhook URL is configured,
 * otherwise logs (simulated) so the demo runs without external connectivity.
 */
@Component
@Slf4j
public class SlackNotifier implements Notifier {

    private final String webhookUrl;
    private final RestClient http = RestClient.create();

    public SlackNotifier(@Value("${aiops.integrations.slack.webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override public String channel() { return "slack"; }
    @Override public boolean live() { return webhookUrl != null && !webhookUrl.isBlank(); }
    @Override public String detail() { return live() ? "incoming webhook" : "log only (set aiops.integrations.slack.webhook-url)"; }

    @Override
    public void send(Incident i, String message) {
        if (!live()) { log.info("[SLACK #incidents] {}", message); return; }
        http.post().uri(webhookUrl)
                .body(Map.of("text", ":rotating_light: " + message))
                .retrieve().toBodilessEntity();
        log.info("[SLACK] delivered incident #{}", i.getId());
    }
}
