package com.sentinel.aiops.service.state;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import java.util.Set;

public class ClosedState implements IncidentState {
    public IncidentStatus status() { return IncidentStatus.CLOSED; }
    public Set<IncidentStatus> allowedTransitions() { return Set.of(); } // terminal
    public void onEnter(Incident i) { i.touch(); }
}
