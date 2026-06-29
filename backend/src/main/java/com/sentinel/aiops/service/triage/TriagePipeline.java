package com.sentinel.aiops.service.triage;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.service.ai.AiAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles and runs the triage chain.
 *
 * <p>The nested {@link Builder} is a <b>Builder pattern</b>: it accumulates
 * handlers and links them into a chain in {@code build()}, keeping construction
 * separate from execution.</p>
 */
@Service
@Slf4j
public class TriagePipeline {

    private final AiAnalysisService ai;

    public TriagePipeline(AiAnalysisService ai) { this.ai = ai; }

    /** Run the default AI triage chain over a freshly reported incident. */
    public List<String> triage(Incident incident) {
        TriageHandler chain = new Builder()
                .add(new SeverityClassificationHandler(ai))
                .add(new EnrichmentHandler(ai))
                .add(new RootCauseHandler(ai))
                .add(new AutoAssignHandler())
                .build();

        TriageContext ctx = new TriageContext(incident);
        chain.handle(ctx);
        log.info("Triage complete for '{}': {}", incident.getTitle(), ctx.getActions());
        return ctx.getActions();
    }

    /** Builder pattern: collect handlers, then wire them into a chain. */
    public static class Builder {
        private final List<TriageHandler> handlers = new ArrayList<>();

        public Builder add(TriageHandler h) { handlers.add(h); return this; }

        public TriageHandler build() {
            if (handlers.isEmpty()) throw new IllegalStateException("empty triage chain");
            for (int i = 0; i < handlers.size() - 1; i++) {
                handlers.get(i).setNext(handlers.get(i + 1));
            }
            return handlers.get(0);
        }
    }
}
