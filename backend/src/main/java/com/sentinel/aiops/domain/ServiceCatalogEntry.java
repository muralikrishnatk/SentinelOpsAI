package com.sentinel.aiops.domain;

import com.sentinel.aiops.domain.enums.ServiceTier;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A monitored service in the catalog. Health is derived from open incidents. */
@Entity
@Table(name = "services")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    /** On-call team responsible for this service. */
    private String ownerTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ServiceTier tier = ServiceTier.TIER2;

    /** Optional runbook URL surfaced to responders. */
    private String runbookUrl;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
