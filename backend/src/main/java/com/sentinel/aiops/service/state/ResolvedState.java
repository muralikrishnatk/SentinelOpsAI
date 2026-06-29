package com.sentinel.aiops.service.state;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import java.time.Instant;
import java.util.Set;

public class ResolvedState implements IncidentState {
    public IncidentStatus status() { return IncidentStatus.RESOLVED; }
    public Set<IncidentStatus> allowedTransitions() {
        return Set.of(IncidentStatus.CLOSED, IncidentStatus.INVESTIGATING); // can reopen
    }
    public void onEnter(Incident i) {
        i.setResolvedAt(Instant.now());
        i.touch();
    }
}
