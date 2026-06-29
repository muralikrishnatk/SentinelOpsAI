package com.sentinel.aiops;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import com.sentinel.aiops.domain.enums.Severity;
import com.sentinel.aiops.service.ai.MockAiProvider;
import com.sentinel.aiops.service.state.IncidentState;
import com.sentinel.aiops.service.state.IncidentStateFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests — no Spring context, fast. */
class CoreLogicTest {

    private final IncidentStateFactory stateFactory = new IncidentStateFactory();
    private final MockAiProvider ai = new MockAiProvider();

    @Test
    void openCanTransitionToAcknowledgedButNotResolved() {
        IncidentState open = stateFactory.forStatus(IncidentStatus.OPEN);
        assertTrue(open.canTransitionTo(IncidentStatus.ACKNOWLEDGED));
        assertFalse(open.canTransitionTo(IncidentStatus.RESOLVED));
    }

    @Test
    void closedIsTerminal() {
        IncidentState closed = stateFactory.forStatus(IncidentStatus.CLOSED);
        assertTrue(closed.allowedTransitions().isEmpty());
    }

    @Test
    void resolvedStateStampsResolvedAt() {
        Incident i = Incident.builder()
                .title("x").affectedService("svc").severity(Severity.SEV3).build();
        assertNull(i.getResolvedAt());
        stateFactory.forStatus(IncidentStatus.RESOLVED).onEnter(i);
        assertNotNull(i.getResolvedAt());
    }

    @Test
    void heuristicClassifierDetectsCriticalOutage() {
        Severity s = ai.classifySeverity("Full outage", "all users affected", "data loss");
        assertEquals(Severity.SEV1, s);
    }

    @Test
    void heuristicRootCauseDetectsLatency() {
        Incident i = Incident.builder()
                .title("slow").affectedService("api").severity(Severity.SEV2)
                .rawSignal("p99 latency timeout deadline").build();
        assertTrue(ai.suggestRootCause(i).toLowerCase().contains("latency"));
    }
}
