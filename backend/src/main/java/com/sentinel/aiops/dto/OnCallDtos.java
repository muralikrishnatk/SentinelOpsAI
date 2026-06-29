package com.sentinel.aiops.dto;

import com.sentinel.aiops.domain.OnCallShift;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class OnCallDtos {
    public record ShiftRequest(@NotBlank String teamName, @NotBlank String username,
                               @NotNull Instant startAt, @NotNull Instant endAt) {}
    public record ShiftView(Long id, String teamName, String username, Instant startAt, Instant endAt) {
        public static ShiftView from(OnCallShift s) {
            return new ShiftView(s.getId(), s.getTeamName(), s.getUsername(), s.getStartAt(), s.getEndAt());
        }
    }
    public record OnCallNow(String teamName, String username, boolean covered) {}
}
