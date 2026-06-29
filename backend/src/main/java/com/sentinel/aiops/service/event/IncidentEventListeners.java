package com.sentinel.aiops.service.event;

import com.sentinel.aiops.service.notification.NotificationService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <b>Observer pattern.</b> Spring's application-event mechanism is a built-in
 * implementation of Observer: the service layer publishes events without
 * knowing who listens, and these {@code @EventListener} methods (observers)
 * react independently. Adding a new reaction means adding a method — no change
 * to the publisher.
 */
@Component
@Slf4j
public class IncidentEventListeners {

    private final MeterRegistry meterRegistry;     // metrics observer (SRE)
    private final NotificationService notifications; // notification observer

    public IncidentEventListeners(MeterRegistry meterRegistry, NotificationService notifications) {
        this.meterRegistry = meterRegistry;
        this.notifications = notifications;
    }

    /** Observer 1: emit metrics for every created incident. */
    @EventListener
    public void onCreatedMetrics(IncidentCreatedEvent e) {
        meterRegistry.counter("aiops.incident.created",
                "severity", e.getIncident().getSeverity().name(),
                "service", e.getIncident().getAffectedService()).increment();
    }

    /** Observer 2: audit log for created incidents. */
    @EventListener
    public void onCreatedAudit(IncidentCreatedEvent e) {
        log.info("AUDIT created incident #{} [{}] {}",
                e.getIncident().getId(), e.getIncident().getSeverity(), e.getIncident().getTitle());
    }

    /** Observer 3: fire notifications asynchronously (non-blocking). */
    @Async
    @EventListener
    public void onCreatedNotify(IncidentCreatedEvent e) {
        notifications.notifyForIncident(e.getIncident());
    }

    /** Observer 4: metrics + audit for status transitions. */
    @EventListener
    public void onStatusChanged(IncidentStatusChangedEvent e) {
        meterRegistry.counter("aiops.incident.transition",
                "from", e.getFrom().name(), "to", e.getTo().name()).increment();
        log.info("AUDIT incident #{} transition {} -> {}",
                e.getIncident().getId(), e.getFrom(), e.getTo());
    }
}
