package com.sentinel.aiops.dto;

import com.sentinel.aiops.domain.enums.IncidentStatus;
import jakarta.validation.constraints.NotNull;

/** Request to move an incident to a new lifecycle state (State pattern). */
public record StatusTransitionRequest(
        @NotNull IncidentStatus targetStatus,
        String assignee,
        String note
) {}
