package com.sentinel.aiops.resilience;

import com.sentinel.aiops.service.ai.AiAnalysisService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom Actuator health indicator that surfaces the active AI provider and
 * whether the platform is running in degraded (local-fallback) mode. Appears
 * under {@code /actuator/health} — useful for dashboards and k8s probes.
 */
@Component
public class AiHealthIndicator implements HealthIndicator {

    private final AiAnalysisService ai;

    public AiHealthIndicator(AiAnalysisService ai) { this.ai = ai; }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("activeProvider", ai.activeProviderName())
                .withDetail("remote", ai.primaryIsRemote())
                .withDetail("note", ai.primaryIsRemote()
                        ? "Remote LLM active; circuit breaker guards failures"
                        : "Local heuristic provider (offline-capable)")
                .build();
    }
}
