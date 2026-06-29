package com.sentinel.aiops.integration.remediation;

import com.sentinel.aiops.domain.enums.RunbookActionType;
import com.sentinel.aiops.telemetry.TrafficGenerator;
import org.springframework.stereotype.Component;

/**
 * Default executor: emits an audit line and, for mitigating actions, clears the
 * injected synthetic fault — closing the loop so the demo metrics recover. Also
 * used whenever dry-run is enabled.
 */
@Component
public class SimulatedRemediationExecutor implements RemediationExecutor {

    private final TrafficGenerator traffic;

    public SimulatedRemediationExecutor(TrafficGenerator traffic) { this.traffic = traffic; }

    @Override public String mode() { return "simulated"; }
    @Override public boolean live() { return false; }
    @Override public String detail() { return "in-process simulation (dry-run safe)"; }

    @Override
    public ExecResult execute(RunbookActionType action, String service, String params) {
        String p = params == null || params.isBlank() ? "" : " (" + params + ")";
        boolean mitigating = switch (action) {
            case RESTART_SERVICE, SCALE_OUT, FAILOVER, ROLLBACK_DEPLOY -> true;
            default -> false;
        };
        if (mitigating) traffic.clearFault(service);
        String verb = switch (action) {
            case RESTART_SERVICE -> "Restarted";
            case SCALE_OUT -> "Scaled out";
            case CLEAR_CACHE -> "Cleared cache for";
            case FAILOVER -> "Failed over";
            case ROLLBACK_DEPLOY -> "Rolled back";
            case RUN_DIAGNOSTIC -> "Ran diagnostic on";
        };
        return new ExecResult("[sim] " + verb + " " + service + p, mitigating);
    }
}
