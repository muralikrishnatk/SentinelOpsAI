package com.sentinel.aiops.dto;

import com.sentinel.aiops.domain.TimelineEvent;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class TimelineDtos {

    public record CommentRequest(@NotBlank String message) {}

    public record TimelineView(Long id, String type, String message,
                               String actor, Instant createdAt) {
        public static TimelineView from(TimelineEvent e) {
            return new TimelineView(e.getId(), e.getType().name(),
                    e.getMessage(), e.getActor(), e.getCreatedAt());
        }
    }
}
