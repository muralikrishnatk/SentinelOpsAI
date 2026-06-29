package com.sentinel.aiops.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * One entry in a team's on-call rotation: {@code username} is the responder on call
 * from {@code startAt} to {@code endAt}. "Who is on call now" = the shift covering now.
 */
@Entity
@Table(name = "oncall_shifts", indexes = {
        @Index(name = "idx_shift_team", columnList = "team_name"),
        @Index(name = "idx_shift_window", columnList = "start_at,end_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnCallShift {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(nullable = false)
    private String username;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;
}
