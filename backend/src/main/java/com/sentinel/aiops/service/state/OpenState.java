package com.sentinel.aiops.service.state;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import java.util.Set;

public class OpenState implements IncidentState {
    public IncidentStatus status() { return IncidentStatus.OPEN; }
    public Set<IncidentStatus> allowedTransitions() {
        return Set.of(IncidentStatus.ACKNOWLEDGED, IncidentStatus.CLOSED);
    }
    public void onEnter(Incident i) { i.touch(); }
}
