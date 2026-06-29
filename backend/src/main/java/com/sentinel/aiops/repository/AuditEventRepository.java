package com.sentinel.aiops.repository;

import com.sentinel.aiops.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findByActorContainingIgnoreCaseOrActionContainingIgnoreCase(
            String actor, String action, Pageable pageable);
}
