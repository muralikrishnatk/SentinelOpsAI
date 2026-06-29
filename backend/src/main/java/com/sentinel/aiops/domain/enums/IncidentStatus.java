package com.sentinel.aiops.domain.enums;

/** Lifecycle status of an incident. Drives the State pattern. */
public enum IncidentStatus {
    OPEN,
    ACKNOWLEDGED,
    INVESTIGATING,
    RESOLVED,
    CLOSED
}
