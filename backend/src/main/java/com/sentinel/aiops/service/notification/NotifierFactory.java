package com.sentinel.aiops.service.notification;

import com.sentinel.aiops.domain.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <b>Factory pattern.</b> Produces the set of notifier channels appropriate for
 * a given severity. SEV1 pages on-call; lower severities use lighter channels.
 * Each produced notifier is wrapped with the retry {@link RetryingNotifierDecorator}.
 */
@Component
public class NotifierFactory {

    private final Map<String, Notifier> byChannel;

    public NotifierFactory(List<Notifier> notifiers) {
        this.byChannel = notifiers.stream()
                .collect(Collectors.toMap(Notifier::channel, Function.identity()));
    }

    public List<Notifier> notifiersFor(Severity severity) {
        List<String> channels = switch (severity) {
            case SEV1 -> List.of("pagerduty", "slack", "webhook", "email");
            case SEV2 -> List.of("slack", "webhook", "email");
            case SEV3 -> List.of("slack");
            case SEV4 -> List.of("email");
        };
        return channels.stream()
                .map(byChannel::get)
                .filter(java.util.Objects::nonNull)
                .map(n -> (Notifier) new RetryingNotifierDecorator(n, 3)) // decorate
                .toList();
    }
}
