package com.sentinel.aiops.domain.enums;

/** Automated remediation actions a runbook step can perform. */
public enum RunbookActionType {
    RESTART_SERVICE, SCALE_OUT, CLEAR_CACHE, FAILOVER, ROLLBACK_DEPLOY, RUN_DIAGNOSTIC
}
