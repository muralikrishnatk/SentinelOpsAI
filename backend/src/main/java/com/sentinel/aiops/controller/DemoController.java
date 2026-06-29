package com.sentinel.aiops.controller;

import com.sentinel.aiops.telemetry.SliSourceProvider;
import com.sentinel.aiops.telemetry.TrafficGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demo controls to drive the live closed-loop: inject a fault to spike a service's
 * error rate/latency (which burns its error budget and triggers the SLO engine),
 * or clear it manually. Remediation clears faults automatically.
 */
@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo / Telemetry", description = "Fault injection and telemetry status")
public class DemoController {

    private final TrafficGenerator traffic;
    private final SliSourceProvider sli;

    public DemoController(TrafficGenerator traffic, SliSourceProvider sli) {
        this.traffic = traffic;
        this.sli = sli;
    }

    public record FaultRequest(String service, Double errorRate, Integer extraLatencyMs, Integer durationSec) {}

    @PostMapping("/fault")
    @PreAuthorize("hasAnyRole('RESPONDER','ADMIN')")
    @Operation(summary = "Inject a fault (spikes errors/latency for a window)")
    public Map<String, Object> inject(@RequestBody FaultRequest req) {
        traffic.injectFault(req.service(),
                req.errorRate() == null ? 0.25 : req.errorRate(),
                req.extraLatencyMs() == null ? 400 : req.extraLatencyMs(),
                req.durationSec() == null ? 600 : req.durationSec());
        return Map.of("service", req.service(), "faulted", true);
    }

    @PostMapping("/clear/{service}")
    @PreAuthorize("hasAnyRole('RESPONDER','ADMIN')")
    @Operation(summary = "Clear an injected fault")
    public Map<String, Object> clear(@PathVariable String service) {
        return Map.of("service", service, "cleared", traffic.clearFault(service));
    }

    @GetMapping("/telemetry")
    @Operation(summary = "Telemetry status: active SLI source + per-service fault state")
    public Map<String, Object> telemetry() {
        Map<String, Boolean> faults = new LinkedHashMap<>();
        Arrays.stream(traffic.services()).forEach(s -> faults.put(s.trim(), traffic.hasFault(s.trim())));
        return Map.of(
                "sliSource", sli.activeName(),
                "usingPrometheus", sli.usingPrometheus(),
                "services", Arrays.stream(traffic.services()).map(String::trim).toList(),
                "faults", faults);
    }
}
