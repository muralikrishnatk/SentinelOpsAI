package com.sentinel.aiops.domain;

import com.sentinel.aiops.domain.enums.RunbookActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An automated remediation runbook bound to a service. When that service breaches
 * (via a burn-rate alert), the runbook can auto-execute its ordered steps — with
 * guardrails (optional human approval, rate limiting).
 */
@Entity
@Table(name = "runbooks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Runbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String service;

    /** If true, runs automatically on trigger; else creates a PENDING_APPROVAL execution. */
    @Column(nullable = false)
    @Builder.Default
    private boolean autoExecute = false;

    /** Guardrail: max auto-executions per hour for this runbook. */
    @Column(nullable = false)
    @Builder.Default
    private int maxExecutionsPerHour = 3;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "runbook_steps", joinColumns = @JoinColumn(name = "runbook_id"))
    @OrderColumn(name = "step_order")
    @Builder.Default
    private List<Step> steps = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Embeddable
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Step {
        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private RunbookActionType action;
        /** Free-form parameter, e.g. target replicas or cache name. */
        private String params;
    }
}
