package com.sentinel.aiops.service.state;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.IncidentStatus;

import java.util.Set;

/**
 * <b>State pattern.</b> Each lifecycle status is a state object that knows which
 * transitions are legal and what side effects occur on entry. This replaces a
 * sprawling switch statement and makes the state machine self-documenting.
 */
public interface IncidentState {

    IncidentStatus status();

    /** Statuses this state is allowed to move to. */
    Set<IncidentStatus> allowedTransitions();

    /** Side effects applied to the incident when entering this state. */
    void onEnter(Incident incident);

    default boolean canTransitionTo(IncidentStatus target) {
        return allowedTransitions().contains(target);
    }
}
