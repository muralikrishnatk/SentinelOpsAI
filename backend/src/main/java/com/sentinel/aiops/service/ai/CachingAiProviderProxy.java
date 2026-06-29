package com.sentinel.aiops.service.ai;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.Severity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Proxy pattern.</b> A surrogate for a real {@link AiProvider} that adds an
 * in-memory cache in front of the (potentially expensive / remote) delegate.
 * Identical inputs return cached results, cutting cost and latency — and it
 * implements the same interface, so callers can't tell it apart from the real
 * provider.
 */
public class CachingAiProviderProxy implements AiProvider {

    private final AiProvider delegate;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public CachingAiProviderProxy(AiProvider delegate) {
        this.delegate = delegate;
    }

    @Override public String name() { return "cached(" + delegate.name() + ")"; }
    @Override public boolean isRemote() { return delegate.isRemote(); }

    @Override
    public String summarize(Incident i) {
        return cache.computeIfAbsent("sum:" + i.getId() + ":" + i.getUpdatedAt(),
                k -> delegate.summarize(i));
    }

    @Override
    public Severity classifySeverity(String title, String description, String rawSignal) {
        // classification is cheap to recompute and rarely repeated; delegate directly
        return delegate.classifySeverity(title, description, rawSignal);
    }

    @Override
    public String suggestRootCause(Incident i) {
        return cache.computeIfAbsent("rc:" + i.getId() + ":" + i.getUpdatedAt(),
                k -> delegate.suggestRootCause(i));
    }

    @Override
    public String answer(String question, List<Incident> context) {
        return cache.computeIfAbsent("ans:" + question.hashCode() + ":" + context.size(),
                k -> delegate.answer(question, context));
    }

    @Override
    public String generatePostmortem(Incident i, List<String> timeline) {
        return cache.computeIfAbsent("pm:" + i.getId() + ":" + timeline.size(),
                k -> delegate.generatePostmortem(i, timeline));
    }

    public void invalidate() { cache.clear(); }
}
