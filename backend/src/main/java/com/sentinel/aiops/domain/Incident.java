package com.sentinel.aiops.domain;

import com.sentinel.aiops.domain.enums.IncidentStatus;
import com.sentinel.aiops.domain.enums.Severity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Core domain entity representing a production incident.
 * Built via the {@code IncidentBuilder} (Builder pattern) — Lombok's
 * {@code @Builder} generates a fluent, immutable-friendly builder.
 */
@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    /** The service or system affected (e.g. "checkout-api"). */
    @Column(nullable = false)
    private String affectedService;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    /** AI-generated concise summary. Null until AI enrichment runs. */
    @Column(length = 4000)
    private String aiSummary;

    /** AI-suggested probable root cause. */
    @Column(length = 4000)
    private String aiRootCause;

    /** Free-form raw signal/log snippet that triggered the incident. */
    @Column(length = 8000)
    private String rawSignal;

    private String assignee;

    /** Username of whoever reported the incident (or "alertmanager"). */
    private String reporter;

    /** AI-generated postmortem document (markdown). Populated on demand. */
    @Column(length = 16000)
    private String postmortem;

    // ---- SLA tracking ----
    /** Target minutes to acknowledge, derived from severity at creation. */
    private Integer slaAckTargetMinutes;
    /** Target minutes to resolve, derived from severity at creation. */
    private Integer slaResolveTargetMinutes;
    @Builder.Default
    private boolean slaAckBreached = false;
    @Builder.Default
    private boolean slaResolveBreached = false;

    private Instant acknowledgedAt;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    private Instant resolvedAt;

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
