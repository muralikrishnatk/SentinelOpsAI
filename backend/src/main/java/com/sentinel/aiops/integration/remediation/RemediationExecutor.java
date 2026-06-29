package com.sentinel.aiops.integration.remediation;

import com.sentinel.aiops.domain.enums.RunbookActionType;

/**
 * <b>Strategy pattern.</b> Carries out a remediation action. Implementations adapt
 * to a target: a simulated executor (demo / dry-run), a generic automation webhook,
 * or the Kubernetes API. Selection is config-driven via {@link RemediationExecutorFactory}.
 */
public interface RemediationExecutor {
    String mode();
    boolean live();
    String detail();
    ExecResult execute(RunbookActionType action, String service, String params);

    record ExecResult(String line, boolean mitigated) {}
}
