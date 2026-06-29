package com.sentinel.aiops.telemetry;

import org.springframework.stereotype.Component;

import java.time.Duration;

/** Computes SLIs from the in-process {@link TrafficStore} (always available). */
@Component
public class InMemorySliSource implements SliSource {

    private final TrafficStore store;

    public InMemorySliSource(TrafficStore store) { this.store = store; }

    @Override public String name() { return "in-memory"; }
    @Override public boolean isRemote() { return false; }

    @Override
    public double errorRatio(String service, Duration window) {
        return store.errorRatio(service, window);
    }

    @Override
    public double slowRatio(String service, Duration window, int thresholdMs) {
        return store.slowRatio(service, window, thresholdMs);
    }
}
