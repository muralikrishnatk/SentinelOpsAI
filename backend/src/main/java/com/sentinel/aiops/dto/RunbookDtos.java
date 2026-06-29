package com.sentinel.aiops.dto;

import com.sentinel.aiops.domain.RemediationExecution;
import com.sentinel.aiops.domain.Runbook;
import com.sentinel.aiops.domain.enums.RemediationStatus;
import com.sentinel.aiops.domain.enums.RunbookActionType;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class RunbookDtos {

    public record StepReq(RunbookActionType action, String params) {}

    public record RunbookRequest(
            @NotBlank String name,
            @NotBlank String service,
            boolean autoExecute,
            Integer maxExecutionsPerHour,
            List<StepReq> steps) {}

    public record StepView(RunbookActionType action, String params) {}

    public record RunbookView(
            Long id, String name, String service, boolean autoExecute,
            int maxExecutionsPerHour, List<StepView> steps) {
        public static RunbookView from(Runbook r) {
            return new RunbookView(r.getId(), r.getName(), r.getService(), r.isAutoExecute(),
                    r.getMaxExecutionsPerHour(),
                    r.getSteps().stream().map(s -> new StepView(s.getAction(), s.getParams())).toList());
        }
    }

    public record ExecutionView(
            Long id, Long runbookId, String runbookName, Long incidentId, String service,
            RemediationStatus status, String triggeredBy, String approvedBy, String log,
            Instant createdAt, Instant finishedAt) {
        public static ExecutionView from(RemediationExecution e) {
            return new ExecutionView(e.getId(), e.getRunbookId(), e.getRunbookName(), e.getIncidentId(),
                    e.getService(), e.getStatus(), e.getTriggeredBy(), e.getApprovedBy(), e.getLog(),
                    e.getCreatedAt(), e.getFinishedAt());
        }
    }
}
