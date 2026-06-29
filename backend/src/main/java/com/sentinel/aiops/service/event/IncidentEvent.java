package com.sentinel.aiops.service.event;

import com.sentinel.aiops.domain.Incident;

/** Base type for incident domain events (Observer pattern subject payload). */
public abstract class IncidentEvent {
    private final Incident incident;
    protected IncidentEvent(Incident incident) { this.incident = incident; }
    public Incident getIncident() { return incident; }
}
