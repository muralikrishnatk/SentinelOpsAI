package com.sentinel.aiops.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** Immutable audit record of a state-changing action — an enterprise compliance need. */
@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_at", columnList = "at"),
        @Index(name = "idx_audit_actor", columnList = "actor")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actor;            // username or "system"
    private String role;

    @Column(nullable = false)
    private String action;           // HTTP method + path, or a logical action
    private String target;           // resource id / path
    private Integer status;          // HTTP status / outcome code
    private String ip;

    @Column(length = 1000)
    private String detail;

    @Column(nullable = false)
    @Builder.Default
    private Instant at = Instant.now();
}
