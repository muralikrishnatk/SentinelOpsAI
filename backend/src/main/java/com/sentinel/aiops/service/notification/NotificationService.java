package com.sentinel.aiops.service.notification;

import com.sentinel.aiops.domain.Incident;
import org.springframework.stereotype.Service;

/** Thin orchestration over the notifier factory + decorated strategies. */
@Service
public class NotificationService {

    private final NotifierFactory factory;

    public NotificationService(NotifierFactory factory) { this.factory = factory; }

    public void notifyForIncident(Incident incident) {
        String message = String.format("[%s] %s on %s — %s",
                incident.getSeverity(), incident.getTitle(),
                incident.getAffectedService(),
                incident.getAiSummary() == null ? "(no summary)" : incident.getAiSummary());
        factory.notifiersFor(incident.getSeverity())
               .forEach(n -> n.send(incident, message));
    }
}
