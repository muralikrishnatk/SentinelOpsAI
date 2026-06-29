package com.sentinel.aiops.service.correlation;

import com.sentinel.aiops.domain.AlertRecord;
import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.Slo;
import com.sentinel.aiops.domain.enums.BurnTier;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import com.sentinel.aiops.domain.enums.Severity;
import com.sentinel.aiops.domain.enums.TimelineEventType;
import com.sentinel.aiops.dto.CreateIncidentRequest;
import com.sentinel.aiops.dto.IncidentResponse;
import com.sentinel.aiops.repository.AlertRecordRepository;
import com.sentinel.aiops.repository.IncidentRepository;
import com.sentinel.aiops.service.IncidentService;
import com.sentinel.aiops.service.realtime.SseService;
import com.sentinel.aiops.service.remediation.RemediationService;
import com.sentinel.aiops.service.timeline.TimelineService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Alert correlation & deduplication — the alert-fatigue fix.
 *
 * <p>A burn-rate alert is first <b>deduplicated</b> (identical alert within a short
 * window is dropped). Surviving alerts are <b>correlated</b>: if the service already
 * has a recent open incident, the alert is attached to it; otherwise one incident is
 * opened and any other un-correlated recent alerts for that service are grouped in.
 * Result: a storm of N alerts becomes one incident, not N pages.</p>
 */
@Service
@Slf4j
public class CorrelationService {

    private final AlertRecordRepository alerts;
    private final IncidentRepository incidents;
    private final IncidentService incidentService;
    private final TimelineService timeline;
    private final SseService sse;
    private final MeterRegistry meters;
    private final RemediationService remediation;

    private final Duration dedupWindow;
    private final Duration correlationWindow;

    public CorrelationService(AlertRecordRepository alerts, IncidentRepository incidents,
                              IncidentService incidentService, TimelineService timeline,
                              SseService sse, MeterRegistry meters, RemediationService remediation,
                              @Value("${aiops.correlation.dedup-window:5m}") String dedupWindow,
                              @Value("${aiops.correlation.window:30m}") String correlationWindow) {
        this.alerts = alerts;
        this.incidents = incidents;
        this.incidentService = incidentService;
        this.timeline = timeline;
        this.sse = sse;
        this.meters = meters;
        this.remediation = remediation;
        this.dedupWindow = DurationStyle.detectAndParse(dedupWindow);
        this.correlationWindow = DurationStyle.detectAndParse(correlationWindow);
    }

    @Transactional
    public void handleBurnAlert(Slo slo, BurnTier tier, double burnShort, double burnLong) {
        if (tier == BurnTier.NONE) return;
        String service = slo.getService();
        String dedupKey = service + ":" + slo.getId() + ":" + tier;

        // 1) Deduplicate
        if (!alerts.findByDedupKeyAndFiredAtAfter(dedupKey, Instant.now().minus(dedupWindow)).isEmpty()) {
            meters.counter("aiops.alerts.suppressed", "reason", "dedup").increment();
            return;
        }

        AlertRecord alert = alerts.save(AlertRecord.builder()
                .service(service).sloId(slo.getId()).sloName(slo.getName())
                .tier(tier).burnRateShort(burnShort).burnRateLong(burnLong)
                .dedupKey(dedupKey).build());
        meters.counter("aiops.alerts.fired", "tier", tier.name(), "service", service).increment();

        // 2) Correlate into an existing open incident if one exists for this service
        Instant since = Instant.now().minus(correlationWindow);
        Incident open = incidents.findByAffectedServiceIgnoreCase(service).stream()
                .filter(i -> i.getStatus() != IncidentStatus.RESOLVED && i.getStatus() != IncidentStatus.CLOSED)
                .filter(i -> i.getCreatedAt().isAfter(since))
                .findFirst().orElse(null);

        if (open != null) {
            alert.setIncidentId(open.getId());
            timeline.record(open.getId(), TimelineEventType.ALERT_INGESTED, "correlator",
                    "Correlated burn-rate alert (" + tier + ", short=" + round(burnShort)
                            + "x/long=" + round(burnLong) + "x) into existing incident");
            meters.counter("aiops.alerts.correlated").increment();

            // Escalate the incident if this burn tier is more severe than its current severity.
            Severity newSev = severityFor(tier);
            if (open.getSeverity() == null || newSev.ordinal() < open.getSeverity().ordinal()) {
                open.setSeverity(newSev);
                timeline.record(open.getId(), TimelineEventType.STATUS_CHANGED, "correlator",
                        "Escalated to " + newSev + " (burn tier " + tier + ")");
                meters.counter("aiops.alerts.escalated").increment();
            }

            // Re-attempt remediation for the worsening/sustained fault. Safe to call
            // repeatedly: auto runs are rate-limited and approvals are de-duplicated.
            remediation.onIncidentOpened(open);

            sse.broadcast("incident.updated", Map.of("id", open.getId(), "correlated", true));
            return;
        }

        // 3) Otherwise open one incident and group any loose recent alerts for this service
        IncidentResponse created = incidentService.createFromAlert(new CreateIncidentRequest(
                "[SLO breach] " + slo.getName() + " burning on " + service,
                "Error-budget burn-rate alert. Tier " + tier + ". Short-window burn " + round(burnShort)
                        + "×, long-window burn " + round(burnLong) + "×.",
                service, severityFor(tier),
                "slo=" + slo.getName() + " tier=" + tier + " burnShort=" + burnShort + " burnLong=" + burnLong));

        alert.setIncidentId(created.id());
        List<AlertRecord> loose = alerts.findByServiceAndIncidentIdIsNullAndFiredAtAfter(service, since);
        int grouped = 0;
        for (AlertRecord a : loose) {
            if (a.getId().equals(alert.getId())) continue;
            a.setIncidentId(created.id());
            grouped++;
        }
        if (grouped > 0) {
            timeline.record(created.id(), TimelineEventType.ALERT_INGESTED, "correlator",
                    "Grouped " + grouped + " additional correlated alert(s) for " + service);
            meters.counter("aiops.alerts.correlated").increment(grouped);
        }
        log.info("Opened incident {} from {} burn alert on {} (grouped {} extra)",
                created.id(), tier, service, grouped);
    }

    private Severity severityFor(BurnTier tier) {
        return switch (tier) {
            case FAST -> Severity.SEV1;
            case SLOW -> Severity.SEV2;
            case TICKET -> Severity.SEV3;
            default -> Severity.SEV4;
        };
    }

    private double round(double v) { return Math.round(v * 10.0) / 10.0; }
}