package com.sentinel.aiops.service.triage;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.service.ai.AiAnalysisService;

/** First link: ensure the incident has a severity, using AI when absent. */
public class SeverityClassificationHandler extends AbstractTriageHandler {
    private final AiAnalysisService ai;
    public SeverityClassificationHandler(AiAnalysisService ai) { this.ai = ai; }

    @Override
    protected void doHandle(TriageContext ctx) {
        Incident i = ctx.getIncident();
        if (i.getSeverity() == null) {
            i.setSeverity(ai.classify(i.getTitle(), i.getDescription(), i.getRawSignal()));
            ctx.log("AI classified severity as " + i.getSeverity());
        } else {
            ctx.log("Severity provided by reporter: " + i.getSeverity());
        }
    }
}
