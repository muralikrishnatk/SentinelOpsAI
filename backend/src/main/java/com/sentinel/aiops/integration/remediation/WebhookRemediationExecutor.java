package com.sentinel.aiops.integration.remediation;

import com.sentinel.aiops.domain.enums.RunbookActionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * <b>Adapter</b> that drives a real automation system (e.g. Rundeck, GitHub Actions
 * dispatch, an internal ops API) over HTTP. Live when a webhook URL is configured.
 */
@Component
@Slf4j
public class WebhookRemediationExecutor implements RemediationExecutor {

    private final String url;
    private final RestClient http = RestClient.create();

    public WebhookRemediationExecutor(@Value("${aiops.remediation.webhook-url:}") String url) {
        this.url = url;
    }

    @Override public String mode() { return "webhook"; }
    @Override public boolean live() { return url != null && !url.isBlank(); }
    @Override public String detail() { return live() ? url : "not configured (aiops.remediation.webhook-url)"; }

    @Override
    public ExecResult execute(RunbookActionType action, String service, String params) {
        http.post().uri(url)
                .body(Map.of("action", action.name(), "service", service,
                        "params", params == null ? "" : params))
                .retrieve().toBodilessEntity();
        log.info("[remediation/webhook] {} {} dispatched", action, service);
        return new ExecResult("[webhook] dispatched " + action + " for " + service, true);
    }
}
