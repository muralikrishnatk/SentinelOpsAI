package com.sentinel.aiops.service.remediation;

import com.sentinel.aiops.service.event.IncidentCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Observer that links incident creation to the remediation engine: when any incident
 * opens, matching runbooks auto-execute or queue for approval.
 */
@Component
public class RemediationTrigger {

    private final RemediationService remediation;

    public RemediationTrigger(RemediationService remediation) { this.remediation = remediation; }

    @EventListener
    public void onCreated(IncidentCreatedEvent event) {
        remediation.onIncidentOpened(event.getIncident());
    }
}
