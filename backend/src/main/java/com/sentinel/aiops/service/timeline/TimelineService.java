package com.sentinel.aiops.service.timeline;

import com.sentinel.aiops.domain.TimelineEvent;
import com.sentinel.aiops.domain.enums.TimelineEventType;
import com.sentinel.aiops.repository.TimelineEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Records and reads the per-incident activity timeline. */
@Service
public class TimelineService {

    private final TimelineEventRepository repo;

    public TimelineService(TimelineEventRepository repo) { this.repo = repo; }

    public TimelineEvent record(Long incidentId, TimelineEventType type, String actor, String message) {
        return repo.save(TimelineEvent.builder()
                .incidentId(incidentId)
                .type(type)
                .actor(actor == null ? "system" : actor)
                .message(message)
                .build());
    }

    public List<TimelineEvent> forIncident(Long incidentId) {
        return repo.findByIncidentIdOrderByCreatedAtAsc(incidentId);
    }
}
