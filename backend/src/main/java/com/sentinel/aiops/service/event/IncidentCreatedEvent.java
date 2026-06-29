package com.sentinel.aiops.service.event;

import com.sentinel.aiops.domain.Incident;

public class IncidentCreatedEvent extends IncidentEvent {
    public IncidentCreatedEvent(Incident incident) { super(incident); }
}
