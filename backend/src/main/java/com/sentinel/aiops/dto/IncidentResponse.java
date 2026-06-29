package com.sentinel.aiops.dto;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import com.sentinel.aiops.domain.enums.Severity;

import java.time.Instant;

/** Outbound DTO. Built from an entity via the static {@code from} mapper. */
public record IncidentResponse(
        Long id,
        String title,
        String description,
        String affectedService,
        Severity severity,
        IncidentStatus status,
        String aiSummary,
        String aiRootCause,
        String assignee,
        String reporter,
        boolean hasPostmortem,
        Integer slaAckTargetMinutes,
        Integer slaResolveTargetMinutes,
        boolean slaAckBreached,
        boolean slaResolveBreached,
        Instant acknowledgedAt,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {
    public static IncidentResponse from(Incident i) {
        return new IncidentResponse(
                i.getId(), i.getTitle(), i.getDescription(), i.getAffectedService(),
                i.getSeverity(), i.getStatus(), i.getAiSummary(), i.getAiRootCause(),
                i.getAssignee(), i.getReporter(), i.getPostmortem() != null,
                i.getSlaAckTargetMinutes(), i.getSlaResolveTargetMinutes(),
                i.isSlaAckBreached(), i.isSlaResolveBreached(),
                i.getAcknowledgedAt(), i.getCreatedAt(), i.getUpdatedAt(), i.getResolvedAt());
    }
}
