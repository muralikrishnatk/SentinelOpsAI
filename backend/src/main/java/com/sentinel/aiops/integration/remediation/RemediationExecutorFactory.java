package com.sentinel.aiops.integration.remediation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <b>Factory pattern.</b> Chooses the active {@link RemediationExecutor} from config:
 * {@code aiops.remediation.mode} = simulated | webhook | kubernetes. A {@code dry-run}
 * guardrail (default true) forces the simulated executor regardless of mode, so the
 * platform never takes real infrastructure action until explicitly switched live.
 */
@Component
@Slf4j
public class RemediationExecutorFactory {

    private final SimulatedRemediationExecutor simulated;
    private final WebhookRemediationExecutor webhook;
    private final KubernetesRemediationExecutor kubernetes;

    private final String mode;
    private final boolean dryRun;

    public RemediationExecutorFactory(SimulatedRemediationExecutor simulated,
                                      WebhookRemediationExecutor webhook,
                                      KubernetesRemediationExecutor kubernetes,
                                      @Value("${aiops.remediation.mode:simulated}") String mode,
                                      @Value("${aiops.remediation.dry-run:true}") boolean dryRun) {
        this.simulated = simulated;
        this.webhook = webhook;
        this.kubernetes = kubernetes;
        this.mode = mode;
        this.dryRun = dryRun;
    }

    public RemediationExecutor current() {
        if (dryRun) return simulated;
        return switch (mode == null ? "" : mode.toLowerCase()) {
            case "webhook" -> webhook.live() ? webhook : fallback("webhook");
            case "kubernetes" -> kubernetes.live() ? kubernetes : fallback("kubernetes");
            default -> simulated;
        };
    }

    public boolean dryRun() { return dryRun; }
    public String configuredMode() { return dryRun ? "simulated (dry-run)" : mode; }

    private RemediationExecutor fallback(String requested) {
        log.warn("Remediation mode '{}' selected but not configured; falling back to simulated.", requested);
        return simulated;
    }
}
