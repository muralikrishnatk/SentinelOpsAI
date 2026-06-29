package com.sentinel.aiops.repository;

import com.sentinel.aiops.domain.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {
    List<TimelineEvent> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
}
