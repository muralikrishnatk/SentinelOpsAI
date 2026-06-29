package com.sentinel.aiops.domain;

import com.sentinel.aiops.domain.enums.TimelineEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A persisted activity-feed entry for an incident (audit + collaboration). */
@Entity
@Table(name = "timeline_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimelineEventType type;

    @Column(length = 4000)
    private String message;

    /** Username (or "system"/"ai") that produced this event. */
    private String actor;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
