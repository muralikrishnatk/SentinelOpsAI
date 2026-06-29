package com.sentinel.aiops.telemetry;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates realistic, continuously-changing traffic for the demo services and
 * records it as REAL Micrometer metrics ({@code aiops_demo_requests_total},
 * {@code aiops_demo_request_latency}) plus into {@link TrafficStore}. Prometheus
 * scrapes these; the SLO engine computes burn rate from them.
 *
 * <p>Faults can be injected at runtime to spike error rate / latency — this is what
 * drives a burn-rate alert, the correlated incident, and (after remediation) the
 * recovery. The remediation engine calls {@link #clearFault(String)} to close the loop.</p>
 */
@Component
@Slf4j
public class TrafficGenerator {

    private record Fault(double errorRate, int extraLatencyMs, Instant until) {}

    private final TrafficStore store;
    private final MeterRegistry meters;
    private final boolean enabled;
    private final String[] services;

    // baseline error rate per service (steady-state noise)
    private final Map<String, Double> baseline = new ConcurrentHashMap<>();
    private final Map<String, Fault> faults = new ConcurrentHashMap<>();

    public TrafficGenerator(TrafficStore store, MeterRegistry meters,
                            @Value("${aiops.telemetry.enabled:true}") boolean enabled,
                            @Value("${aiops.telemetry.services:checkout-api,payment-gateway,identity-service,data-platform}") String servicesCsv) {
        this.store = store;
        this.meters = meters;
        this.enabled = enabled;
        this.services = servicesCsv.split(",");
        for (String s : services) baseline.put(s.trim(), 0.002); // ~0.2% steady errors
    }

    /** Fires ~ every second; emits a burst of requests per service. */
    @Scheduled(fixedRate = 1000)
    public void tick() {
        if (!enabled) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (String svc : services) {
            String service = svc.trim();
            int requests = 20 + rnd.nextInt(30); // 20-50 req/s
            Fault f = activeFault(service);
            double errRate = baseline.getOrDefault(service, 0.002) + (f != null ? f.errorRate() : 0.0);
            int extraLatency = f != null ? f.extraLatencyMs() : 0;

            for (int i = 0; i < requests; i++) {
                boolean ok = rnd.nextDouble() >= errRate;
                int latency = 40 + rnd.nextInt(120) + extraLatency + (ok ? 0 : rnd.nextInt(200));
                store.record(service, ok, latency);
                meters.counter("aiops_demo_requests_total",
                        "service", service, "outcome", ok ? "success" : "error").increment();
                Timer.builder("aiops_demo_request_latency")
                        .tag("service", service)
                        .publishPercentileHistogram()
                        .register(meters)
                        .record(latency, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        }
    }

    /** Inject a fault that elevates errors/latency for a window of seconds. */
    public void injectFault(String service, double errorRate, int extraLatencyMs, int durationSec) {
        faults.put(service, new Fault(
                Math.max(0, Math.min(1, errorRate)),
                Math.max(0, extraLatencyMs),
                Instant.now().plusSeconds(durationSec)));
        log.info("Injected fault on {}: errorRate={}, +{}ms for {}s", service, errorRate, extraLatencyMs, durationSec);
    }

    /** Closed-loop: remediation calls this to clear an active fault. */
    public boolean clearFault(String service) {
        boolean had = faults.remove(service) != null;
        if (had) log.info("Cleared fault on {} (remediation)", service);
        return had;
    }

    public boolean hasFault(String service) { return activeFault(service) != null; }

    public String[] services() { return services; }

    private Fault activeFault(String service) {
        Fault f = faults.get(service);
        if (f == null) return null;
        if (Instant.now().isAfter(f.until())) { faults.remove(service); return null; }
        return f;
    }
}
