package com.sentinel.aiops.domain;

import com.sentinel.aiops.domain.enums.SliType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A Service Level Objective. Availability example: 99.9% of requests succeed
 * over a 30-day window. Latency example: 99% of requests under 300ms.
 */
@Entity
@Table(name = "slos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Slo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SliType sliType;

    /** Objective as a percentage, e.g. 99.9 means 99.9% good. */
    @Column(nullable = false)
    private double objectivePercent;

    /** Rolling compliance window in days (typically 28 or 30). */
    @Column(nullable = false)
    @Builder.Default
    private int windowDays = 30;

    /** For LATENCY SLOs: a request is "good" if under this many ms. */
    private Integer latencyThresholdMs;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
