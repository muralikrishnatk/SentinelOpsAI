package com.sentinel.aiops.service.event;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.IncidentStatus;

public class IncidentStatusChangedEvent extends IncidentEvent {
    private final IncidentStatus from;
    private final IncidentStatus to;
    public IncidentStatusChangedEvent(Incident i, IncidentStatus from, IncidentStatus to) {
        super(i); this.from = from; this.to = to;
    }
    public IncidentStatus getFrom() { return from; }
    public IncidentStatus getTo() { return to; }
}
