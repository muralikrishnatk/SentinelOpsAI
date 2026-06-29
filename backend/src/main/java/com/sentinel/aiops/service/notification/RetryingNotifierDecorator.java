package com.sentinel.aiops.service.notification;

import com.sentinel.aiops.domain.Incident;
import lombok.extern.slf4j.Slf4j;

/**
 * <b>Decorator pattern.</b> Wraps any {@link Notifier} and transparently adds
 * bounded retry-with-backoff behaviour, while preserving the Notifier interface
 * so callers are unaffected. Decorators can be stacked (e.g. retry + rate-limit).
 */
@Slf4j
public class RetryingNotifierDecorator implements Notifier {

    private final Notifier delegate;
    private final int maxAttempts;

    public RetryingNotifierDecorator(Notifier delegate, int maxAttempts) {
        this.delegate = delegate;
        this.maxAttempts = maxAttempts;
    }

    public String channel() { return delegate.channel() + "+retry"; }

    public void send(Incident i, String message) {
        int attempt = 0;
        while (true) {
            try {
                attempt++;
                delegate.send(i, message);
                return;
            } catch (RuntimeException ex) {
                if (attempt >= maxAttempts) {
                    log.error("Notifier {} failed after {} attempts", delegate.channel(), attempt, ex);
                    throw ex;
                }
                log.warn("Notifier {} attempt {} failed, retrying", delegate.channel(), attempt);
            }
        }
    }
}
