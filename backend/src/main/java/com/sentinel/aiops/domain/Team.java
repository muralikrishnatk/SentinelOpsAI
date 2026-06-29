package com.sentinel.aiops.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** A team within an organization that owns services and an on-call rotation. */
@Entity
@Table(name = "teams", indexes = @Index(name = "idx_team_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Team {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "organization_id")
    private Long organizationId;

    private String description;
    private String escalationContact;   // fallback if nobody is on call

    @Version
    private Long version;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
