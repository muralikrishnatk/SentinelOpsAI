package com.sentinel.aiops.service.state;

import com.sentinel.aiops.domain.enums.IncidentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * <b>Factory Method pattern.</b> Maps an {@link IncidentStatus} to its
 * corresponding {@link IncidentState} object. Centralises construction so the
 * service layer never instantiates state classes directly.
 */
@Component
public class IncidentStateFactory {

    private final Map<IncidentStatus, IncidentState> states = new EnumMap<>(IncidentStatus.class);

    public IncidentStateFactory() {
        register(new OpenState());
        register(new AcknowledgedState());
        register(new InvestigatingState());
        register(new ResolvedState());
        register(new ClosedState());
    }

    private void register(IncidentState s) { states.put(s.status(), s); }

    public IncidentState forStatus(IncidentStatus status) {
        IncidentState s = states.get(status);
        if (s == null) throw new IllegalStateException("No state registered for " + status);
        return s;
    }
}
