package com.sentinel.aiops.service.ai;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.Severity;

import java.util.List;

/**
 * <b>Strategy pattern.</b> Defines a family of interchangeable AI back-ends.
 * Concrete strategies: {@link MockAiProvider} (offline, heuristic) and
 * {@link AnthropicAiProvider} (remote LLM). The rest of the application
 * depends only on this interface, never on a concrete provider.
 */
public interface AiProvider {

    /** Human-readable provider id, surfaced in API responses. */
    String name();

    /** True if this strategy makes outbound network calls. */
    boolean isRemote();

    /** Produce a concise incident summary. */
    String summarize(Incident incident);

    /** Classify severity from raw signal/title/description. */
    Severity classifySeverity(String title, String description, String rawSignal);

    /** Suggest a probable root cause. */
    String suggestRootCause(Incident incident);

    /** Answer a natural-language question grounded in the supplied incidents. */
    String answer(String question, List<Incident> context);

    /** Generate a postmortem document (markdown) from an incident + its timeline. */
    String generatePostmortem(Incident incident, List<String> timeline);
}
