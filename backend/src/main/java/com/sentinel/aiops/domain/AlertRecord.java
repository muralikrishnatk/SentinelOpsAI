package com.sentinel.aiops.domain;

import com.sentinel.aiops.domain.enums.BurnTier;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A fired burn-rate alert. Persisted so alerts can be correlated/deduplicated
 * into incidents and shown as history.
 */
@Entity
@Table(name = "alert_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String service;

    private Long sloId;
    private String sloName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BurnTier tier;

    private double burnRateShort;
    private double burnRateLong;

    /** A stable key for deduplication (service + slo + tier). */
    @Column(nullable = false)
    private String dedupKey;

    /** Incident this alert was correlated into (nullable until correlated). */
    private Long incidentId;

    @Column(nullable = false)
    @Builder.Default
    private Instant firedAt = Instant.now();
}
