package com.sentinel.aiops.service.state;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import java.util.Set;

public class InvestigatingState implements IncidentState {
    public IncidentStatus status() { return IncidentStatus.INVESTIGATING; }
    public Set<IncidentStatus> allowedTransitions() {
        return Set.of(IncidentStatus.RESOLVED, IncidentStatus.ACKNOWLEDGED);
    }
    public void onEnter(Incident i) { i.touch(); }
}
