package com.sentinel.aiops.integration.health;

import com.sentinel.aiops.service.ai.AiAnalysisService;
import org.springframework.stereotype.Component;

@Component
public class AiProbe extends AbstractIntegrationProbe {

    private final AiAnalysisService ai;

    public AiProbe(AiAnalysisService ai) { this.ai = ai; }

    @Override protected String name() { return "AI provider"; }
    @Override protected String category() { return "ai"; }

    @Override
    protected Result check() {
        return ai.primaryIsRemote()
                ? new Result(IntegrationState.UP, ai.activeProviderName())
                : new Result(IntegrationState.SIMULATED, ai.activeProviderName() + " (offline heuristic; set ANTHROPIC_API_KEY)");
    }
}
