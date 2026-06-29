package com.sentinel.aiops.service.triage;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.service.ai.AiAnalysisService;

/** Second link: attach an AI-generated summary. */
public class EnrichmentHandler extends AbstractTriageHandler {
    private final AiAnalysisService ai;
    public EnrichmentHandler(AiAnalysisService ai) { this.ai = ai; }

    @Override
    protected void doHandle(TriageContext ctx) {
        Incident i = ctx.getIncident();
        i.setAiSummary(ai.summarize(i));
        ctx.log("AI summary generated");
    }
}
