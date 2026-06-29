package com.sentinel.aiops.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Chooses the active {@link SliSource}: Prometheus when configured and reachable,
 * otherwise the in-memory store. Re-probes periodically so the platform survives
 * Prometheus going up/down — the same graceful-degradation story as the AI layer.
 */
@Component
@Slf4j
public class SliSourceProvider {

    private final InMemorySliSource inMemory;
    private final ObjectProvider<PrometheusSliSource> prometheusProvider;

    private volatile SliSource active;
    private volatile long lastProbe = 0;
    private static final long PROBE_INTERVAL_MS = 15_000;

    public SliSourceProvider(InMemorySliSource inMemory,
                             ObjectProvider<PrometheusSliSource> prometheusProvider) {
        this.inMemory = inMemory;
        this.prometheusProvider = prometheusProvider;
        this.active = inMemory;
    }

    public SliSource active() {
        maybeProbe();
        return active;
    }

    public String activeName() { return active().name(); }
    public boolean usingPrometheus() { return active().isRemote(); }

    public double errorRatio(String service, Duration window) {
        return active().errorRatio(service, window);
    }

    public double slowRatio(String service, Duration window, int thresholdMs) {
        return active().slowRatio(service, window, thresholdMs);
    }

    private void maybeProbe() {
        long now = System.currentTimeMillis();
        if (now - lastProbe < PROBE_INTERVAL_MS) return;
        lastProbe = now;
        PrometheusSliSource prom = prometheusProvider.getIfAvailable();
        if (prom != null && prom.healthy()) {
            if (!active.isRemote()) log.info("SLI source -> Prometheus");
            active = prom;
        } else {
            if (active.isRemote()) log.warn("Prometheus unreachable; SLI source -> in-memory");
            active = inMemory;
        }
    }
}
