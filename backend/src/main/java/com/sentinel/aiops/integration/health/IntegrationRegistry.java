package com.sentinel.aiops.integration.health;

import com.sentinel.aiops.integration.remediation.RemediationExecutorFactory;
import com.sentinel.aiops.service.notification.Notifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Facade</b> over every external integration's health. Aggregates active probes
 * (Template Method) with the configured-vs-live state of the notification channels,
 * the remediation executor, and inbound ingestion — one call powers the Integrations page.
 */
@Component
public class IntegrationRegistry {

    private final List<IntegrationProbe> probes;
    private final List<Notifier> notifiers;
    private final RemediationExecutorFactory executors;

    public IntegrationRegistry(List<IntegrationProbe> probes, List<Notifier> notifiers,
                               RemediationExecutorFactory executors) {
        this.probes = probes;
        this.notifiers = notifiers;
        this.executors = executors;
    }

    public List<IntegrationStatus> all() {
        List<IntegrationStatus> out = new ArrayList<>();

        // Active probes (Prometheus, AI, ...)
        probes.forEach(p -> out.add(p.probe()));

        // Outbound notification channels
        for (Notifier n : notifiers) {
            out.add(new IntegrationStatus(
                    "Notify: " + n.channel(), "notification",
                    n.live() ? IntegrationState.UP : IntegrationState.SIMULATED,
                    n.detail(), 0));
        }

        // Remediation executor
        var exec = executors.current();
        out.add(new IntegrationStatus(
                "Remediation executor", "remediation",
                executors.dryRun() ? IntegrationState.SIMULATED
                        : (exec.live() ? IntegrationState.UP : IntegrationState.SIMULATED),
                executors.configuredMode() + " · " + exec.detail(), 0));

        // Inbound ingestion (always available)
        out.add(new IntegrationStatus(
                "Ingest: Alertmanager", "ingestion", IntegrationState.UP,
                "POST /api/ingest/alertmanager", 0));

        return out;
    }
}
