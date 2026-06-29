package com.sentinel.aiops.service.sla;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.Severity;
import com.sentinel.aiops.domain.enums.TimelineEventType;
import com.sentinel.aiops.repository.IncidentRepository;
import com.sentinel.aiops.service.realtime.SseService;
import com.sentinel.aiops.service.timeline.TimelineService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * SLA policy + enforcement.
 *
 * <p>Targets are keyed by severity (a small Strategy-style policy table). A
 * scheduled monitor flags acknowledge/resolve breaches, records them on the
 * timeline, emits a metric, and pushes a live update — the kind of automated
 * SLO enforcement an SRE team actually relies on.</p>
 */
@Service
@Slf4j
public class SlaService {

    // ack target / resolve target in minutes, by severity
    private static final Map<Severity, int[]> TARGETS = Map.of(
            Severity.SEV1, new int[]{5, 60},
            Severity.SEV2, new int[]{15, 240},
            Severity.SEV3, new int[]{60, 1440},
            Severity.SEV4, new int[]{240, 4320});

    private final IncidentRepository incidents;
    private final TimelineService timeline;
    private final SseService sse;
    private final MeterRegistry meters;

    public SlaService(IncidentRepository incidents, TimelineService timeline,
                      SseService sse, MeterRegistry meters) {
        this.incidents = incidents;
        this.timeline = timeline;
        this.sse = sse;
        this.meters = meters;
    }

    /** Applied at incident creation to stamp SLA targets. */
    public void applyTargets(Incident incident) {
        int[] t = TARGETS.getOrDefault(incident.getSeverity(), new int[]{60, 1440});
        incident.setSlaAckTargetMinutes(t[0]);
        incident.setSlaResolveTargetMinutes(t[1]);
    }

    /** Runs every 30s; flags any newly-breached SLAs. */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void monitor() {
        Instant now = Instant.now();
        for (Incident i : incidents.findAll()) {
            boolean changed = false;

            if (!i.isSlaAckBreached() && i.getAcknowledgedAt() == null
                    && i.getSlaAckTargetMinutes() != null
                    && minutesSince(i.getCreatedAt(), now) > i.getSlaAckTargetMinutes()) {
                i.setSlaAckBreached(true);
                changed = true;
                timeline.record(i.getId(), TimelineEventType.SLA_BREACH, "sla-monitor",
                        "Acknowledge SLA breached (target " + i.getSlaAckTargetMinutes() + "m)");
                meters.counter("aiops.sla.breach", "kind", "ack",
                        "severity", i.getSeverity().name()).increment();
            }

            if (!i.isSlaResolveBreached() && i.getResolvedAt() == null
                    && i.getSlaResolveTargetMinutes() != null
                    && minutesSince(i.getCreatedAt(), now) > i.getSlaResolveTargetMinutes()) {
                i.setSlaResolveBreached(true);
                changed = true;
                timeline.record(i.getId(), TimelineEventType.SLA_BREACH, "sla-monitor",
                        "Resolve SLA breached (target " + i.getSlaResolveTargetMinutes() + "m)");
                meters.counter("aiops.sla.breach", "kind", "resolve",
                        "severity", i.getSeverity().name()).increment();
            }

            if (changed) {
                incidents.save(i);
                sse.broadcast("incident.updated", Map.of("id", i.getId(), "slaBreach", true));
            }
        }
    }

    private long minutesSince(Instant from, Instant now) {
        return Duration.between(from, now).toMinutes();
    }
}
