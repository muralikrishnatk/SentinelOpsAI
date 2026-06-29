package com.sentinel.aiops.service.triage;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.Severity;
import com.sentinel.aiops.service.ai.AiAnalysisService;

/** Third link: for high-severity incidents, request an AI root-cause hypothesis. */
public class RootCauseHandler extends AbstractTriageHandler {
    private final AiAnalysisService ai;
    public RootCauseHandler(AiAnalysisService ai) { this.ai = ai; }

    @Override
    protected void doHandle(TriageContext ctx) {
        Incident i = ctx.getIncident();
        if (i.getSeverity() == Severity.SEV1 || i.getSeverity() == Severity.SEV2) {
            i.setAiRootCause(ai.suggestRootCause(i));
            ctx.log("AI root-cause analysis attached (high severity)");
        } else {
            ctx.log("Root-cause analysis skipped (low severity)");
        }
    }
}
