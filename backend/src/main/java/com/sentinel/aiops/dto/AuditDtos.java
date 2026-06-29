package com.sentinel.aiops.dto;

import com.sentinel.aiops.domain.AuditEvent;
import java.time.Instant;

public class AuditDtos {
    public record AuditView(Long id, String actor, String role, String action, String target,
                            Integer status, String ip, String detail, Instant at) {
        public static AuditView from(AuditEvent e) {
            return new AuditView(e.getId(), e.getActor(), e.getRole(), e.getAction(), e.getTarget(),
                    e.getStatus(), e.getIp(), e.getDetail(), e.getAt());
        }
    }
}
