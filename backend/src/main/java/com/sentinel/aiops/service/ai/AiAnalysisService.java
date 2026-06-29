package com.sentinel.aiops.service.ai;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.Severity;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <b>Facade pattern.</b> A single, simple entry point over the entire AI
 * subsystem (factory + strategies + proxies + resilience). Callers don't know
 * whether an answer came from a remote LLM or the local fallback.
 *
 * <p>The cached proxies are built here from the {@link AiProviderFactory} output
 * rather than as Spring beans — this keeps the proxies out of the factory's own
 * {@code List<AiProvider>} (avoiding a circular dependency) while still
 * demonstrating Factory → Proxy composition.</p>
 *
 * <p>Each remote-capable method is guarded by a Resilience4j
 * <b>circuit breaker</b> (with retry). When the breaker opens — or any error
 * occurs — execution falls back to the always-available local provider. This is
 * the classic SRE resilience story: degrade gracefully, never hard-fail.</p>
 */
@Service
@Slf4j
public class AiAnalysisService {

    private final CachingAiProviderProxy primary;
    private final CachingAiProviderProxy local;

    public AiAnalysisService(AiProviderFactory factory) {
        this.primary = new CachingAiProviderProxy(factory.primary());
        this.local = new CachingAiProviderProxy(factory.local());
        log.info("AiAnalysisService primary={}, local={}", primary.name(), local.name());
    }

    public String activeProviderName() { return primary.name(); }

    @CircuitBreaker(name = "aiProvider", fallbackMethod = "summarizeFallback")
    @Retry(name = "aiProvider")
    public String summarize(Incident i) {
        return primary.summarize(i);
    }
    @SuppressWarnings("unused")
    private String summarizeFallback(Incident i, Throwable t) {
        log.warn("AI summarize fell back to local: {}", t.toString());
        return local.summarize(i);
    }

    @CircuitBreaker(name = "aiProvider", fallbackMethod = "classifyFallback")
    @Retry(name = "aiProvider")
    public Severity classify(String title, String description, String rawSignal) {
        return primary.classifySeverity(title, description, rawSignal);
    }
    @SuppressWarnings("unused")
    private Severity classifyFallback(String title, String description, String rawSignal, Throwable t) {
        log.warn("AI classify fell back to local: {}", t.toString());
        return local.classifySeverity(title, description, rawSignal);
    }

    @CircuitBreaker(name = "aiProvider", fallbackMethod = "rootCauseFallback")
    @Retry(name = "aiProvider")
    public String suggestRootCause(Incident i) {
        return primary.suggestRootCause(i);
    }
    @SuppressWarnings("unused")
    private String rootCauseFallback(Incident i, Throwable t) {
        log.warn("AI root-cause fell back to local: {}", t.toString());
        return local.suggestRootCause(i);
    }

    @CircuitBreaker(name = "aiProvider", fallbackMethod = "answerFallback")
    @Retry(name = "aiProvider")
    public String answer(String question, List<Incident> context) {
        return primary.answer(question, context);
    }
    @SuppressWarnings("unused")
    private String answerFallback(String question, List<Incident> context, Throwable t) {
        log.warn("AI answer fell back to local: {}", t.toString());
        return local.answer(question, context);
    }

    /** True when the primary path is remote (so a fallback would be a degradation). */
    public boolean primaryIsRemote() { return primary.isRemote(); }

    @CircuitBreaker(name = "aiProvider", fallbackMethod = "postmortemFallback")
    @Retry(name = "aiProvider")
    public String generatePostmortem(Incident i, List<String> timeline) {
        return primary.generatePostmortem(i, timeline);
    }
    @SuppressWarnings("unused")
    private String postmortemFallback(Incident i, List<String> timeline, Throwable t) {
        log.warn("AI postmortem fell back to local: {}", t.toString());
        return local.generatePostmortem(i, timeline);
    }
}
