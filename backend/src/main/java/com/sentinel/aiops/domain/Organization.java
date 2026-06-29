package com.sentinel.aiops.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** A tenant on the platform. Foundation for multi-tenancy (full row-scoping is on the roadmap). */
@Entity
@Table(name = "organizations", indexes = @Index(name = "idx_org_slug", columnList = "slug", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String plan;             // e.g. FREE / TEAM / ENTERPRISE

    @Version
    private Long version;            // optimistic locking

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
