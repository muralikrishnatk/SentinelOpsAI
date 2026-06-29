package com.sentinel.aiops.integration.health;

import com.sentinel.aiops.telemetry.SliSourceProvider;
import org.springframework.stereotype.Component;

@Component
public class PrometheusProbe extends AbstractIntegrationProbe {

    private final SliSourceProvider sli;

    public PrometheusProbe(SliSourceProvider sli) { this.sli = sli; }

    @Override protected String name() { return "Prometheus"; }
    @Override protected String category() { return "telemetry"; }

    @Override
    protected Result check() {
        if (sli.usingPrometheus())
            return new Result(IntegrationState.UP, sli.activeName());
        return new Result(IntegrationState.SIMULATED, "in-memory SLI source (set aiops.prometheus.url)");
    }
}
