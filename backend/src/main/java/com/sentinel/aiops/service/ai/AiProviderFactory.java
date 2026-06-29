package com.sentinel.aiops.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <b>Factory pattern.</b> Chooses the active {@link AiProvider} strategy at
 * runtime. Spring injects every provider bean on the classpath; the factory
 * prefers a remote provider when one is available and enabled, otherwise it
 * falls back to the local heuristic provider.
 */
@Component
@Slf4j
public class AiProviderFactory {

    private final List<AiProvider> providers;
    private final boolean preferRemote;

    public AiProviderFactory(List<AiProvider> providers,
                             @Value("${aiops.ai.prefer-remote:true}") boolean preferRemote) {
        this.providers = providers;
        this.preferRemote = preferRemote;
        log.info("AI providers registered: {}", providers.stream().map(AiProvider::name).toList());
    }

    /** The primary provider used under normal conditions. */
    public AiProvider primary() {
        if (preferRemote) {
            return providers.stream()
                    .filter(AiProvider::isRemote)
                    .findFirst()
                    .orElseGet(this::local);
        }
        return local();
    }

    /** The always-available offline fallback. */
    public AiProvider local() {
        return providers.stream()
                .filter(p -> !p.isRemote())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No local AI provider registered"));
    }
}
