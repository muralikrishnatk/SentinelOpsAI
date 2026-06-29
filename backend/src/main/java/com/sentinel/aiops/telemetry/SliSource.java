package com.sentinel.aiops.telemetry;

import java.time.Duration;

/**
 * <b>Strategy pattern.</b> Source of Service Level Indicators. The platform reads
 * SLIs from Prometheus in production and from an in-process store otherwise, so the
 * SLO engine is identical either way.
 */
public interface SliSource {
    String name();
    boolean isRemote();

    /** Fraction of failed requests over the window (0..1). */
    double errorRatio(String service, Duration window);

    /** Fraction of requests slower than thresholdMs over the window (0..1). */
    double slowRatio(String service, Duration window, int thresholdMs);
}
