package com.sentinel.aiops.service.state;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import java.util.Set;

public class AcknowledgedState implements IncidentState {
    public IncidentStatus status() { return IncidentStatus.ACKNOWLEDGED; }
    public Set<IncidentStatus> allowedTransitions() {
        return Set.of(IncidentStatus.INVESTIGATING, IncidentStatus.RESOLVED);
    }
    public void onEnter(Incident i) { i.touch(); }
}
