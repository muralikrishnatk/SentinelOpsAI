package com.sentinel.aiops.service.remediation;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.RemediationExecution;
import com.sentinel.aiops.domain.Runbook;
import com.sentinel.aiops.domain.enums.RemediationStatus;
import com.sentinel.aiops.domain.enums.TimelineEventType;
import com.sentinel.aiops.dto.RunbookDtos.*;
import com.sentinel.aiops.exception.NotFoundException;
import com.sentinel.aiops.integration.remediation.RemediationExecutor;
import com.sentinel.aiops.integration.remediation.RemediationExecutorFactory;
import com.sentinel.aiops.repository.RemediationExecutionRepository;
import com.sentinel.aiops.repository.RunbookRepository;
import com.sentinel.aiops.service.command.CommandInvoker;
import com.sentinel.aiops.service.command.IncidentCommand;
import com.sentinel.aiops.service.realtime.SseService;
import com.sentinel.aiops.service.timeline.TimelineService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Closed-loop auto-remediation. When an incident opens, matching runbooks either
 * auto-execute (with rate-limit + approval guardrails) or queue for human approval.
 * Mitigating actions actually clear the underlying fault via {@link TrafficGenerator},
 * so the metrics — and the error budget — visibly recover. Each step runs through the
 * {@link CommandInvoker} (Command pattern) for timing + audit.
 */
@Service
@Slf4j
public class RemediationService {

    private final RunbookRepository runbooks;
    private final RemediationExecutionRepository executions;
    private final CommandInvoker invoker;
    private final TimelineService timeline;
    private final SseService sse;
    private final MeterRegistry meters;
    private final RemediationExecutorFactory executors;

    public RemediationService(RunbookRepository runbooks, RemediationExecutionRepository executions,
                              CommandInvoker invoker, TimelineService timeline, SseService sse,
                              MeterRegistry meters, RemediationExecutorFactory executors) {
        this.runbooks = runbooks;
        this.executions = executions;
        this.invoker = invoker;
        this.timeline = timeline;
        this.sse = sse;
        this.meters = meters;
        this.executors = executors;
    }

    @Transactional
    public RunbookView create(RunbookRequest req) {
        List<Runbook.Step> steps = new ArrayList<>();
        if (req.steps() != null)
            req.steps().forEach(s -> steps.add(Runbook.Step.builder().action(s.action()).params(s.params()).build()));
        Runbook rb = Runbook.builder()
                .name(req.name()).service(req.service()).autoExecute(req.autoExecute())
                .maxExecutionsPerHour(req.maxExecutionsPerHour() == null ? 3 : req.maxExecutionsPerHour())
                .steps(steps).build();
        return RunbookView.from(runbooks.save(rb));
    }

    @Transactional(readOnly = true)
    public List<RunbookView> list() { return runbooks.findAll().stream().map(RunbookView::from).toList(); }

    @Transactional(readOnly = true)
    public List<ExecutionView> recentExecutions() {
        return executions.findTop50ByOrderByCreatedAtDesc().stream().map(ExecutionView::from).toList();
    }

    /**
     * Called when an incident opens AND on subsequent correlated/escalated burn alerts.
     * Safe to call repeatedly: auto runs are bounded by the per-runbook hourly rate limit,
     * and approval-required runbooks are queued at most once per incident (no duplicates).
     */
    @Transactional
    public void onIncidentOpened(Incident incident) {
        for (Runbook rb : runbooks.findByService(incident.getAffectedService())) {
            if (rb.isAutoExecute()) {
                long recent = executions.countByRunbookIdAndCreatedAtAfter(
                        rb.getId(), Instant.now().minus(1, ChronoUnit.HOURS));
                if (recent >= rb.getMaxExecutionsPerHour()) {
                    // Rate limit hit — skip quietly (no SKIPPED spam on repeated triggers).
                    continue;
                }
                execute(rb, incident, "auto", null);
            } else {
                // Don't queue a second approval if one is already pending for this incident.
                if (executions.existsByRunbookIdAndIncidentIdAndStatus(
                        rb.getId(), incident.getId(), RemediationStatus.PENDING_APPROVAL)) {
                    continue;
                }
                RemediationExecution queued = record(rb, incident, RemediationStatus.PENDING_APPROVAL,
                        "auto", null, "Awaiting approval to run runbook '" + rb.getName() + "'.");
                timeline.record(incident.getId(), TimelineEventType.STATUS_CHANGED, "remediation",
                        "Runbook '" + rb.getName() + "' pending approval (execution #" + queued.getId() + ")");
            }
        }
    }

    /** Approve and run a previously queued execution. */
    @Transactional
    public ExecutionView approve(Long executionId, String username) {
        RemediationExecution ex = executions.findById(executionId)
                .orElseThrow(() -> new NotFoundException("Execution " + executionId + " not found"));
        if (ex.getStatus() != RemediationStatus.PENDING_APPROVAL)
            throw new IllegalArgumentException("Execution is not pending approval");
        Runbook rb = runbooks.findById(ex.getRunbookId())
                .orElseThrow(() -> new NotFoundException("Runbook not found"));
        ex.setApprovedBy(username);
        return ExecutionView.from(runSteps(rb, ex));
    }

    private RemediationExecution execute(Runbook rb, Incident incident, String triggeredBy, String approvedBy) {
        RemediationExecution ex = record(rb, incident, RemediationStatus.RUNNING, triggeredBy, approvedBy, "");
        return runSteps(rb, ex);
    }

    private RemediationExecution runSteps(Runbook rb, RemediationExecution ex) {
        ex.setStatus(RemediationStatus.RUNNING);
        RemediationExecutor executor = executors.current();
        StringBuilder logBuf = new StringBuilder();
        logBuf.append("Executor: ").append(executor.mode())
                .append(executor.live() ? " (LIVE)" : " (simulated)").append("\n");
        boolean mitigated = false;
        try {
            for (Runbook.Step step : rb.getSteps()) {
                RemediationExecutor.ExecResult r = invoker.run(new IncidentCommand<>() {
                    @Override public RemediationExecutor.ExecResult execute() {
                        return executor.execute(step.getAction(), rb.getService(), step.getParams());
                    }
                    @Override public String describe() { return "Remediate(" + step.getAction() + " on " + rb.getService() + ")"; }
                });
                logBuf.append(r.line()).append("\n");
                if (r.mitigated()) mitigated = true;
            }
            if (mitigated)
                logBuf.append("✓ Mitigation applied to ").append(rb.getService()).append("; metrics recovering.\n");
            ex.setStatus(RemediationStatus.SUCCEEDED);
            meters.counter("aiops.remediation.run", "result", "succeeded", "mode", executor.mode()).increment();
        } catch (Exception e) {
            logBuf.append("✗ Step failed: ").append(e.getMessage()).append("\n");
            ex.setStatus(RemediationStatus.FAILED);
            meters.counter("aiops.remediation.run", "result", "failed", "mode", executor.mode()).increment();
        }
        ex.setLog(logBuf.toString());
        ex.setFinishedAt(Instant.now());
        executions.save(ex);
        if (ex.getIncidentId() != null) {
            timeline.record(ex.getIncidentId(), TimelineEventType.STATUS_CHANGED, "remediation",
                    "Runbook '" + rb.getName() + "' " + ex.getStatus() + " via " + executor.mode());
            sse.broadcast("incident.updated", java.util.Map.of("id", ex.getIncidentId(), "remediated", true));
        }
        return ex;
    }

    private RemediationExecution record(Runbook rb, Incident incident, RemediationStatus status,
                                        String triggeredBy, String approvedBy, String log) {
        return executions.save(RemediationExecution.builder()
                .runbookId(rb.getId()).runbookName(rb.getName())
                .incidentId(incident == null ? null : incident.getId())
                .service(rb.getService()).status(status)
                .triggeredBy(triggeredBy).approvedBy(approvedBy).log(log).build());
    }
}