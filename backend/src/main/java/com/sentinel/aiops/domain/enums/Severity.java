package com.sentinel.aiops.domain.enums;

/** Incident severity. SEV1 is most critical. */
public enum Severity {
    SEV1, // critical - full outage
    SEV2, // major - significant degradation
    SEV3, // minor - limited impact
    SEV4  // informational
}
