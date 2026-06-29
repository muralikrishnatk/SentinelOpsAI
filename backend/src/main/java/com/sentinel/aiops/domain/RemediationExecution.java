package com.sentinel.aiops.domain;

import com.sentinel.aiops.domain.enums.RemediationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** An attempt to run a runbook against an incident; auditable. */
@Entity
@Table(name = "remediation_executions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RemediationExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long runbookId;
    private String runbookName;
    private Long incidentId;
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RemediationStatus status;

    private String triggeredBy;   // "auto" or a username
    private String approvedBy;

    @Column(length = 8000)
    private String log;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    private Instant finishedAt;
}
