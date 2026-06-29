package com.sentinel.aiops.service.notification;

import com.sentinel.aiops.domain.Incident;

/**
 * <b>Strategy pattern.</b> A family of interchangeable notification channels.
 * The notification service depends on this abstraction, not on Slack/PagerDuty/etc.
 * Channels are real integrations: when configured with an endpoint they make live
 * HTTP calls; otherwise they log (simulated) so demos work offline.
 */
public interface Notifier {
    String channel();
    void send(Incident incident, String message);

    /** True when a real endpoint/credential is configured (live, not simulated). */
    default boolean live() { return false; }
    /** Human-readable target for the integrations page. */
    default String detail() { return channel(); }
}
