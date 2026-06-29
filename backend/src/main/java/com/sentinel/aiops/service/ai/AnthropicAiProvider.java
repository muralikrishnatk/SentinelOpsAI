package com.sentinel.aiops.service.ai;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * <b>Adapter pattern.</b> Adapts the Anthropic Messages HTTP API to the
 * platform's {@link AiProvider} interface so the rest of the system can treat
 * a remote LLM exactly like any other strategy.
 *
 * <p>Only instantiated when {@code aiops.ai.anthropic.api-key} is set, so the
 * default build needs no secrets. Wrapped by the circuit breaker in
 * {@link AiAnalysisService} for resilience.</p>
 */
@Component
@Slf4j
@ConditionalOnExpression("'${aiops.ai.anthropic.api-key:}' != ''")
public class AnthropicAiProvider implements AiProvider {

    private final RestClient client;
    private final String model;

    public AnthropicAiProvider(
            @Value("${aiops.ai.anthropic.api-key}") String apiKey,
            @Value("${aiops.ai.anthropic.model:claude-sonnet-4-6}") String model,
            @Value("${aiops.ai.anthropic.base-url:https://api.anthropic.com}") String baseUrl) {
        this.model = model;
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("AnthropicAiProvider initialised with model {}", model);
    }

    @Override public String name() { return "anthropic:" + model; }
    @Override public boolean isRemote() { return true; }

    @Override
    public String summarize(Incident i) {
        return complete("""
            You are an SRE assistant. Summarise this incident in 2-3 sentences for an
            on-call engineer. Be specific and actionable.
            Title: %s
            Service: %s
            Description: %s
            Raw signal: %s
            """.formatted(i.getTitle(), i.getAffectedService(),
                          nz(i.getDescription()), nz(i.getRawSignal())));
    }

    @Override
    public Severity classifySeverity(String title, String description, String rawSignal) {
        String resp = complete("""
            Classify the severity of this incident as exactly one of: SEV1, SEV2, SEV3, SEV4.
            SEV1 = full/critical outage, SEV2 = major degradation, SEV3 = minor, SEV4 = informational.
            Respond with ONLY the label.
            Title: %s
            Description: %s
            Signal: %s
            """.formatted(title, nz(description), nz(rawSignal)));
        try {
            return Severity.valueOf(resp.trim().toUpperCase().replaceAll("[^A-Z0-9]", ""));
        } catch (Exception e) {
            return Severity.SEV3; // safe default
        }
    }

    @Override
    public String suggestRootCause(Incident i) {
        return complete("""
            You are an SRE assistant. Given this incident, list the 2-3 most probable root
            causes and the first diagnostic step for each. Be concise.
            Title: %s
            Service: %s
            Description: %s
            Raw signal: %s
            """.formatted(i.getTitle(), i.getAffectedService(),
                          nz(i.getDescription()), nz(i.getRawSignal())));
    }

    @Override
    public String answer(String question, List<Incident> context) {
        StringBuilder ctx = new StringBuilder();
        context.stream().limit(40).forEach(i -> ctx.append("- [#%d|%s|%s|%s] %s%n"
                .formatted(i.getId(), i.getSeverity(), i.getStatus(),
                           i.getAffectedService(), i.getTitle())));
        return complete("""
            You are an incident-management analyst. Using ONLY the incident list below,
            answer the question concisely. If the data is insufficient, say so.

            INCIDENTS:
            %s
            QUESTION: %s
            """.formatted(ctx, question));
    }

    @Override
    public String generatePostmortem(Incident i, List<String> timeline) {
        StringBuilder tl = new StringBuilder();
        timeline.forEach(line -> tl.append("- ").append(line).append("\n"));
        return complete("""
            You are an SRE writing a blameless postmortem in markdown. Use these sections:
            Summary, Impact, Root Cause, Timeline, Resolution, Action Items.
            Be concrete and blameless. Incident:
            Title: %s
            Service: %s
            Severity: %s
            Summary so far: %s
            Root cause so far: %s
            Timeline:
            %s
            """.formatted(i.getTitle(), i.getAffectedService(), i.getSeverity(),
                          nz(i.getAiSummary()), nz(i.getAiRootCause()), tl.toString()));
    }

    /** Single point that adapts our prompt to the Anthropic /v1/messages contract. */
    @SuppressWarnings("unchecked")
    private String complete(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 1024,
                "messages", List.of(Map.of("role", "user", "content", prompt)));
        Map<String, Object> response = client.post()
                .uri("/v1/messages")
                .body(body)
                .retrieve()
                .body(Map.class);
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        return content.stream()
                .filter(b -> "text".equals(b.get("type")))
                .map(b -> (String) b.get("text"))
                .reduce("", (a, b) -> a + b)
                .trim();
    }

    private static String nz(String s) { return s == null ? "(none)" : s; }
}
